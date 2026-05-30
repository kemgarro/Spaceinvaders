---
name: c-client
description: Guía específica para implementar el cliente jugador y espectador en C del proyecto spaCEinvaders. Consultar al escribir cualquier código C del cliente, al diseñar structs para entidades del juego, al implementar el loop de renderizado, al integrar con sockets TCP del servidor, al leer UART del Pico, o al organizar archivos .c/.h. Incluye recomendación de librería gráfica (raylib), estructura del archivo constants.h obligatorio, plantillas de structs, manejo del game loop, y técnicas para conexión con servidor.
---

# Guía del cliente C — spaCEinvaders

El cliente C es la **interfaz gráfica del juego**. No tiene lógica de juego; solo renderiza lo que el servidor le indica y reporta inputs al servidor.

## 1. Principios de diseño

### 1.1 Paradigma imperativo
- **Funciones libres** organizadas por módulo (no clases simuladas).
- **Structs como contenedores de datos**, no como objetos con métodos.
- **Estado explícito**: las funciones reciben el estado como parámetro, no lo asumen global.
- **Flujo lineal**: leer input → enviar al servidor → recibir actualización → renderizar.

### 1.2 Lo que NO hacer
- ❌ Simular OOP con structs llenos de function pointers (parece OOP a medias).
- ❌ Variables globales para todo "porque es C".
- ❌ Lógica de juego en el cliente (el servidor es la autoridad).
- ❌ Funciones gigantes que mezclan render, input y networking.

## 2. Librería gráfica recomendada: raylib

### 2.1 Por qué raylib
- API simple en C puro.
- Una sola dependencia.
- Instalación fácil en Linux, Windows, Mac.
- Documentación clara.
- Suficiente para un juego 2D estilo Space Invaders.

### 2.2 Instalación

**Ubuntu/Debian:**
```bash
sudo apt install libraylib-dev
# o si no está en repos:
git clone https://github.com/raysan5/raylib
cd raylib/src && make && sudo make install
```

**Compilación:**
```bash
gcc src/*.c -o client -lraylib -lm -lpthread -ldl
```

### 2.3 Alternativas
- **SDL2:** más control, más complejo. Usar si raylib no está disponible.
- **ncurses:** terminal-based. Funcional pero feo. Solo como último recurso.

## 3. Estructura del archivo `constants.h` (OBLIGATORIO)

Este archivo es **explícitamente requerido por el enunciado**. Debe contener TODAS las constantes del proyecto.

```c
#ifndef CONSTANTS_H
#define CONSTANTS_H

/* ================ CONFIGURACIÓN DE VENTANA ================ */
#define VENTANA_ANCHO         800
#define VENTANA_ALTO          600
#define VENTANA_TITULO        "spaCEinvaders"
#define FPS                   60

/* ================ CONFIGURACIÓN DE RED ================ */
#define SERVIDOR_IP           "127.0.0.1"
#define SERVIDOR_PUERTO       5555
#define BUFFER_MENSAJE        512
#define TIMEOUT_PING_SEG      5

/* ================ CONFIGURACIÓN UART (PICO) ================ */
#define UART_DISPOSITIVO      "/dev/ttyACM0"
#define UART_BAUDRATE         115200

/* ================ PUNTAJES DE ALIENS ================ */
#define PUNTOS_SQUID          10
#define PUNTOS_CRAB           20
#define PUNTOS_OCTOPUS        40

/* ================ CONFIGURACIÓN DEL JUEGO ================ */
#define VIDAS_INICIALES       3
#define MAX_BUNKERS           4
#define MAX_ALIENS            100
#define MAX_BULLETS           50

/* ================ DIMENSIONES DE ENTIDADES ================ */
#define CANNON_ANCHO          40
#define CANNON_ALTO           20
#define ALIEN_ANCHO           30
#define ALIEN_ALTO            20
#define BALA_ANCHO            3
#define BALA_ALTO             10
#define BUNKER_ANCHO          60
#define BUNKER_ALTO           40

/* ================ VELOCIDADES (px por frame) ================ */
#define CANNON_VELOCIDAD      5
#define BALA_VELOCIDAD        8

/* ================ COLORES ================ */
#define COLOR_FONDO           BLACK
#define COLOR_CANNON          GREEN
#define COLOR_SQUID           WHITE
#define COLOR_CRAB            CYAN
#define COLOR_OCTOPUS         MAGENTA
#define COLOR_UFO             RED
#define COLOR_BUNKER          GREEN
#define COLOR_TEXTO           WHITE

/* ================ ESTADOS DE BUNKERS ================ */
#define BUNKER_INTACTO        100
#define BUNKER_DANIADO        70
#define BUNKER_CRITICO        40
#define BUNKER_DESTRUIDO      0

/* ================ TIPOS DE ALIEN ================ */
#define TIPO_SQUID            1
#define TIPO_CRAB             2
#define TIPO_OCTOPUS          3

/* ================ TIPOS DE CLIENTE ================ */
#define TIPO_JUGADOR          "PLAYER"
#define TIPO_ESPECTADOR       "SPECTATOR"

/* ================ COMANDOS DEL PROTOCOLO ================ */
#define CMD_HELLO             "HELLO"
#define CMD_MOVE              "MOVE"
#define CMD_FIRE              "FIRE"
#define CMD_BYE               "BYE"
#define CMD_PING              "PING"

#endif /* CONSTANTS_H */
```

## 4. Structs principales (`structs.h`)

```c
#ifndef STRUCTS_H
#define STRUCTS_H

#include <stdbool.h>
#include "constants.h"

/* Posición y dimensión */
typedef struct {
    int x, y;
    int ancho, alto;
} Rect;

/* Alien */
typedef struct {
    int id;
    int tipo;       /* TIPO_SQUID, TIPO_CRAB, TIPO_OCTOPUS */
    int puntos;
    Rect rect;
    bool vivo;
} Alien;

/* OVNI */
typedef struct {
    int id;
    Rect rect;
    int direccion;  /* +1 = derecha, -1 = izquierda */
    int puntos_base;
    bool activo;
} Ovni;

/* Bala */
typedef struct {
    int id;
    Rect rect;
    int duenio;     /* 0 = alien, >0 = jugador N */
    bool activa;
} Bala;

/* Bunker */
typedef struct {
    int id;
    Rect rect;
    int salud_pct;  /* 100, 70, 40, 0 */
} Bunker;

/* Cañón del jugador */
typedef struct {
    int player_id;
    Rect rect;
    int vidas;
    int puntaje;
} Cannon;

/* Nodo de lista enlazada genérica */
typedef struct Nodo {
    void *dato;
    struct Nodo *siguiente;
} Nodo;

/* Lista enlazada */
typedef struct {
    Nodo *cabeza;
    int tamanio;
} Lista;

/* Estado completo del juego del lado del cliente */
typedef struct {
    Lista *aliens;
    Lista *balas;
    Lista *ovnis;
    Bunker bunkers[MAX_BUNKERS];
    Cannon cannons[2];     /* Soporta 2 jugadores mínimo */
    int num_jugadores;
    int my_player_id;
    int wave_actual;
    bool game_over;
    bool conectado;
} EstadoJuego;

/* Conexión con el servidor */
typedef struct {
    int socket_fd;
    char buffer_lectura[BUFFER_MENSAJE];
    int buffer_pos;
} ConexionServidor;

/* Conexión UART con el Pico */
typedef struct {
    int fd;
    bool activo;
} ConexionPico;

#endif /* STRUCTS_H */
```

## 5. Lista enlazada (`linked_list.h` / `linked_list.c`)

El proyecto exige listas como estructura. Implementación genérica con `void*`:

```c
/* linked_list.h */
#ifndef LINKED_LIST_H
#define LINKED_LIST_H

#include "structs.h"

Lista *lista_crear(void);
void lista_destruir(Lista *lista, void (*liberar_dato)(void *));
void lista_agregar(Lista *lista, void *dato);
void *lista_obtener(Lista *lista, int indice);
bool lista_eliminar_por_id(Lista *lista, int id_buscado, 
                            int (*obtener_id)(void *),
                            void (*liberar_dato)(void *));
void lista_iterar(Lista *lista, void (*funcion)(void *, void *), void *contexto);
int lista_tamanio(Lista *lista);

#endif
```

```c
/* linked_list.c */
#include <stdlib.h>
#include "linked_list.h"

Lista *lista_crear(void) {
    Lista *l = malloc(sizeof(Lista));
    if (l == NULL) return NULL;
    l->cabeza = NULL;
    l->tamanio = 0;
    return l;
}

void lista_agregar(Lista *lista, void *dato) {
    if (lista == NULL || dato == NULL) return;
    Nodo *nuevo = malloc(sizeof(Nodo));
    if (nuevo == NULL) return;
    nuevo->dato = dato;
    nuevo->siguiente = lista->cabeza;
    lista->cabeza = nuevo;
    lista->tamanio++;
}

/* ... resto de funciones ... */
```

## 6. Organización del código

### 6.1 Módulos sugeridos

| Archivo | Responsabilidad |
|---------|-----------------|
| `main.c` | Loop principal, inicialización, cleanup |
| `network.c` | Conexión TCP, envío/recepción de mensajes |
| `message_parser.c` | Parser de mensajes del servidor |
| `render.c` | Dibujo con raylib (cada entidad tiene su función) |
| `input.c` | Lectura de teclado y UART, traducción a comandos |
| `uart_reader.c` | Apertura y lectura del puerto serial del Pico |
| `game_state.c` | Aplicación de mensajes del servidor al estado local |
| `linked_list.c` | Implementación de la lista enlazada |

### 6.2 Cada `.c` tiene su `.h` con la API pública.

## 7. Loop principal (`main.c`)

```c
#include <stdio.h>
#include <stdlib.h>
#include "raylib.h"
#include "constants.h"
#include "structs.h"
#include "network.h"
#include "render.h"
#include "input.h"
#include "game_state.h"

int main(int argc, char *argv[]) {
    /* Argumentos: --spectator para modo espectador */
    bool modo_espectador = false;
    if (argc > 1 && strcmp(argv[1], "--spectator") == 0) {
        modo_espectador = true;
    }
    
    /* Inicializar ventana */
    InitWindow(VENTANA_ANCHO, VENTANA_ALTO, VENTANA_TITULO);
    SetTargetFPS(FPS);
    
    /* Inicializar estado del juego */
    EstadoJuego estado;
    estado_inicializar(&estado);
    
    /* Conectar al servidor */
    ConexionServidor conexion;
    if (!conexion_servidor_abrir(&conexion, SERVIDOR_IP, SERVIDOR_PUERTO)) {
        fprintf(stderr, "No se pudo conectar al servidor\n");
        CloseWindow();
        return 1;
    }
    
    /* Enviar HELLO */
    const char *tipo = modo_espectador ? TIPO_ESPECTADOR : TIPO_JUGADOR;
    conexion_servidor_enviar_hello(&conexion, tipo, "Jugador");
    
    /* Abrir conexión con el Pico (solo modo jugador) */
    ConexionPico pico = {0};
    if (!modo_espectador) {
        pico_abrir(&pico, UART_DISPOSITIVO, UART_BAUDRATE);
        /* Si falla, seguir solo con teclado */
    }
    
    /* Game loop */
    while (!WindowShouldClose() && estado.conectado) {
        /* 1. Procesar mensajes pendientes del servidor */
        char mensaje[BUFFER_MENSAJE];
        while (conexion_servidor_recibir(&conexion, mensaje, BUFFER_MENSAJE)) {
            parser_aplicar_mensaje(&estado, mensaje);
        }
        
        /* 2. Procesar input (solo si es jugador) */
        if (!modo_espectador) {
            input_procesar_teclado(&conexion);
            input_procesar_pico(&pico, &conexion);
        }
        
        /* 3. Renderizar */
        BeginDrawing();
        ClearBackground(COLOR_FONDO);
        render_dibujar_estado(&estado);
        EndDrawing();
    }
    
    /* Cleanup */
    conexion_servidor_cerrar(&conexion);
    if (!modo_espectador) pico_cerrar(&pico);
    estado_destruir(&estado);
    CloseWindow();
    
    return 0;
}
```

## 8. Conexión TCP con el servidor

```c
/* network.c */
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <unistd.h>
#include <fcntl.h>
#include <string.h>
#include <errno.h>
#include "network.h"

bool conexion_servidor_abrir(ConexionServidor *con, const char *ip, int puerto) {
    con->socket_fd = socket(AF_INET, SOCK_STREAM, 0);
    if (con->socket_fd < 0) return false;
    
    struct sockaddr_in addr;
    memset(&addr, 0, sizeof(addr));
    addr.sin_family = AF_INET;
    addr.sin_port = htons(puerto);
    inet_pton(AF_INET, ip, &addr.sin_addr);
    
    if (connect(con->socket_fd, (struct sockaddr *)&addr, sizeof(addr)) < 0) {
        close(con->socket_fd);
        return false;
    }
    
    /* No bloqueante para poder mezclar con el loop de juego */
    int flags = fcntl(con->socket_fd, F_GETFL, 0);
    fcntl(con->socket_fd, F_SETFL, flags | O_NONBLOCK);
    
    con->buffer_pos = 0;
    return true;
}

bool conexion_servidor_enviar(ConexionServidor *con, const char *mensaje) {
    size_t len = strlen(mensaje);
    ssize_t enviado = send(con->socket_fd, mensaje, len, 0);
    return enviado == (ssize_t)len;
}

/* Recibe una línea completa (terminada en \n) o retorna false si no hay */
bool conexion_servidor_recibir(ConexionServidor *con, char *out, int max_len) {
    /* Lectura no bloqueante al buffer interno */
    char temp[BUFFER_MENSAJE];
    ssize_t n = recv(con->socket_fd, temp, sizeof(temp) - 1, 0);
    if (n > 0) {
        /* Concatenar al buffer interno */
        if (con->buffer_pos + n < BUFFER_MENSAJE) {
            memcpy(con->buffer_lectura + con->buffer_pos, temp, n);
            con->buffer_pos += n;
        }
    }
    
    /* Buscar \n en el buffer */
    for (int i = 0; i < con->buffer_pos; i++) {
        if (con->buffer_lectura[i] == '\n') {
            int len = i;
            if (len >= max_len) len = max_len - 1;
            memcpy(out, con->buffer_lectura, len);
            out[len] = '\0';
            
            /* Desplazar el resto al inicio del buffer */
            memmove(con->buffer_lectura, con->buffer_lectura + i + 1,
                    con->buffer_pos - i - 1);
            con->buffer_pos -= (i + 1);
            return true;
        }
    }
    
    return false;
}
```

## 9. Lectura del UART (Pico)

```c
/* uart_reader.c */
#include <fcntl.h>
#include <termios.h>
#include <unistd.h>
#include "uart_reader.h"

bool pico_abrir(ConexionPico *p, const char *dispositivo, int baudrate) {
    p->fd = open(dispositivo, O_RDWR | O_NOCTTY | O_NONBLOCK);
    if (p->fd < 0) {
        p->activo = false;
        return false;
    }
    
    struct termios tty;
    tcgetattr(p->fd, &tty);
    cfsetospeed(&tty, B115200);
    cfsetispeed(&tty, B115200);
    tty.c_cflag = (tty.c_cflag & ~CSIZE) | CS8;
    tty.c_cflag |= (CLOCAL | CREAD);
    tty.c_cflag &= ~(PARENB | PARODD);
    tty.c_cflag &= ~CSTOPB;
    tty.c_iflag = IGNBRK;
    tty.c_lflag = 0;
    tty.c_oflag = 0;
    tcsetattr(p->fd, TCSANOW, &tty);
    
    p->activo = true;
    return true;
}

/* Lee un byte si hay disponible. Retorna 0 si no hay nada. */
char pico_leer_byte(ConexionPico *p) {
    if (!p->activo) return 0;
    char c;
    ssize_t n = read(p->fd, &c, 1);
    if (n == 1) return c;
    return 0;
}
```

## 10. Manejo de memoria

- Cada `malloc` debe tener su `free` en el flujo correspondiente.
- Al destruir una lista, liberar también los datos contenidos (pasar el destructor).
- En el cleanup principal, liberar todo: estado, listas, conexiones.
- Usar `valgrind` durante desarrollo para detectar fugas:
  ```bash
  valgrind --leak-check=full ./client
  ```

## 11. Makefile recomendado

```makefile
CC = gcc
CFLAGS = -Wall -Wextra -Iinclude -O2
LDFLAGS = -lraylib -lm -lpthread -ldl

SRCS = $(wildcard src/*.c)
OBJS = $(SRCS:.c=.o)
TARGET = client

all: $(TARGET)

$(TARGET): $(OBJS)
	$(CC) $(OBJS) -o $(TARGET) $(LDFLAGS)

src/%.o: src/%.c
	$(CC) $(CFLAGS) -c $< -o $@

clean:
	rm -f src/*.o $(TARGET)

run: $(TARGET)
	./$(TARGET)

spectator: $(TARGET)
	./$(TARGET) --spectator

.PHONY: all clean run spectator
```

## 12. Checklist de calidad

- [ ] Todas las constantes están en `constants.h`.
- [ ] No hay números mágicos en el código (excepto 0, 1, -1 en flujos obvios).
- [ ] Cada función verifica sus parámetros (NULL checks donde aplique).
- [ ] Cada `malloc` tiene su `free`.
- [ ] Cada `open`/`socket` tiene su `close`.
- [ ] Las funciones reciben el estado como parámetro, no usan globales.
- [ ] El código compila con `-Wall -Wextra` sin warnings.
- [ ] El ejecutable se genera correctamente con `make`.
- [ ] `valgrind` no reporta fugas mayores.
- [ ] El cliente se cierra limpiamente al cerrar la ventana o desconectarse.
