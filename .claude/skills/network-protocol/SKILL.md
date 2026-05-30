---
name: network-protocol
description: Especificación completa del protocolo de comunicación por sockets entre el servidor Java y los clientes C (jugador y espectador), incluyendo formato de mensajes, comandos del administrador, eventos del cliente, y manejo de errores de red. Consultar al implementar cualquier código de red (envío/recepción de sockets, parseo de mensajes, construcción de mensajes), o al integrar el cliente con el servidor. También aplica al protocolo UART entre el Pico y el cliente C.
---

# Protocolo de red — spaCEinvaders

Define el formato exacto de los mensajes intercambiados entre componentes del sistema. Es fundamental que **servidor y cliente acuerden este protocolo** antes de implementar, porque un desacuerdo causa bugs muy difíciles de depurar.

## 1. Decisiones de diseño del protocolo

### 1.1 Transporte
- **Sockets TCP** (no UDP). El orden y la entrega garantizada importan.
- Servidor escucha en puerto **5555** por defecto (configurable).
- Cada mensaje termina con `\n` (newline) para delimitar.

### 1.2 Formato de mensajes
- **Texto plano legible** (no binario). Razones:
  - Fácil de depurar (se puede ver el tráfico con `tcpdump` o `nc`).
  - El enunciado da ejemplos en texto plano (`Crear (1,1,1000)`, `OVNI I-D 1500`).
  - Menor complejidad de implementación.
- **Separador de campos:** `|` (pipe)
- **Codificación:** UTF-8.
- **Estructura general:** `COMMAND|arg1|arg2|...|argN\n`

### 1.3 Sesión

```
1. Cliente abre conexión TCP al servidor.
2. Cliente envía HELLO|<tipo>|<nombre>\n
   - tipo: PLAYER o SPECTATOR
   - nombre: identificador del jugador
3. Servidor responde con WELCOME|<player_id>\n o REJECT|<razón>\n
4. Comienza el intercambio de mensajes del juego.
5. Cualquiera de las partes puede cerrar con BYE\n o cerrando el socket.
```

## 2. Mensajes del servidor → cliente

Estos mensajes describen cambios en el estado del juego. El cliente los aplica para renderizar.

### 2.1 Conexión

| Mensaje | Formato | Descripción |
|---------|---------|-------------|
| Bienvenida | `WELCOME\|<player_id>` | Confirma conexión. `player_id` identifica al cliente (1, 2, ...). |
| Rechazo | `REJECT\|<razón>` | Servidor rechaza al cliente (lleno, error, etc.). |
| Pong | `PONG` | Respuesta a un `PING` de keep-alive. |

### 2.2 Estado inicial del juego

| Mensaje | Formato | Descripción |
|---------|---------|-------------|
| Inicio | `GAME_START\|<wave>` | Indica que inicia una oleada (número de wave). |
| Tamaño del campo | `FIELD\|<width>\|<height>` | Dimensiones del área de juego en coordenadas lógicas. |

### 2.3 Entidades

| Mensaje | Formato | Descripción |
|---------|---------|-------------|
| Crear alien | `ALIEN_CREATE\|<id>\|<x>\|<y>\|<type>\|<pts>` | Crea un alien. `type` ∈ {SQUID, CRAB, OCTOPUS}. `pts` ∈ {10, 20, 40}. |
| Mover alien | `ALIEN_MOVE\|<id>\|<x>\|<y>` | Actualiza posición de un alien. |
| Destruir alien | `ALIEN_KILL\|<id>\|<killer_player_id>` | Alien destruido. `killer_player_id` indica quién lo mató (para puntos). |
| Crear OVNI | `UFO_CREATE\|<id>\|<x>\|<y>\|<dir>\|<base_pts>` | `dir` ∈ {LR, RL} (left-right, right-left). |
| Mover OVNI | `UFO_MOVE\|<id>\|<x>\|<y>` | |
| Destruir OVNI | `UFO_KILL\|<id>\|<killer_player_id>\|<actual_pts>` | `actual_pts` es el valor aleatorio finalmente otorgado. |
| Crear bala | `BULLET_CREATE\|<id>\|<x>\|<y>\|<owner>` | `owner` ∈ {ALIEN, PLAYER_1, PLAYER_2, ...}. |
| Mover bala | `BULLET_MOVE\|<id>\|<x>\|<y>` | |
| Destruir bala | `BULLET_DESTROY\|<id>` | |
| Estado bunker | `BUNKER_STATE\|<id>\|<percent>` | `percent` ∈ {100, 70, 40, 0}. |
| Posición cañón | `CANNON_POS\|<player_id>\|<x>` | Actualiza posición horizontal del cañón. |

### 2.4 Estado del jugador

| Mensaje | Formato | Descripción |
|---------|---------|-------------|
| Vidas | `LIVES\|<player_id>\|<lives>` | Actualiza vidas restantes del jugador. |
| Puntaje | `SCORE\|<player_id>\|<score>` | Actualiza puntaje. |
| Velocidad | `SPEED\|<interval_ms>` | Notifica cambio de velocidad de aliens. |

### 2.5 Eventos especiales

| Mensaje | Formato | Descripción |
|---------|---------|-------------|
| Nueva oleada | `WAVE_START\|<wave_number>` | Inicia nueva oleada. |
| Game over | `GAME_OVER\|<player_id>\|<final_score>` | Un jugador perdió. |
| Game over global | `GAME_END\|<reason>` | Termina la partida completa. |
| Error | `ERROR\|<código>\|<mensaje>` | Error informativo. |

## 3. Mensajes del cliente → servidor

El cliente jugador envía eventos de input y notificaciones.

| Mensaje | Formato | Descripción |
|---------|---------|-------------|
| Hello | `HELLO\|<type>\|<name>` | Identificación inicial. `type` ∈ {PLAYER, SPECTATOR}. |
| Mover izquierda | `MOVE\|L` | El jugador presionó izquierda. |
| Mover derecha | `MOVE\|R` | El jugador presionó derecha. |
| Disparar | `FIRE` | El jugador disparó. |
| Ping | `PING` | Keep-alive. |
| Despedida | `BYE` | Cierre limpio. |

> **Importante:** El cliente NO envía mensajes como `ALIEN_KILL` directamente. El servidor detecta colisiones (la lógica vive en el servidor). El cliente solo envía inputs.
>
> **Excepción:** El enunciado dice "el cliente le informa al servidor cuando un extraterrestre es eliminado". Esto sugiere que el cliente **podría** participar en la detección. Decisión recomendada: el cliente envía `FIRE` y el servidor detecta colisiones contra alien/bunker/etc. Esto evita inconsistencias entre clientes.

## 4. Mensajes del administrador → servidor (consola admin)

El operador del servidor tiene una CLI para inyectar comandos. Estos comandos siguen el formato textual del enunciado y son traducidos internamente.

| Comando humano (enunciado) | Formato interno | Acción |
|----------------------------|-----------------|--------|
| `Crear (X, Y, Pts)` | `ADMIN_CREATE_ALIEN\|x\|y\|pts` | Crea alien. |
| `OVNI I-D 1500` | `ADMIN_CREATE_UFO\|LR\|1500` | Crea OVNI L→R con base 1500. |
| `OVNI D-I 1500` | `ADMIN_CREATE_UFO\|RL\|1500` | Crea OVNI R→L con base 1500. |
| `Velocidad 100` | `ADMIN_SET_SPEED\|100` | Cambia velocidad. |
| `Bunkers 70%` | `ADMIN_SET_BUNKERS\|70` | Setea bunkers al 70%. |

> El admin puede aceptar tanto el formato del enunciado como el formato interno. Lo importante es que el formato del enunciado **se acepte literalmente** para la revisión.

## 5. Ejemplos de sesión completa

### 5.1 Conexión y primer alien

```
[Cliente abre socket en server:5555]

Cliente → Servidor: HELLO|PLAYER|Juan
Servidor → Cliente: WELCOME|1
Servidor → Cliente: FIELD|800|600
Servidor → Cliente: GAME_START|1
Servidor → Cliente: BUNKER_STATE|1|100
Servidor → Cliente: BUNKER_STATE|2|100
Servidor → Cliente: BUNKER_STATE|3|100
Servidor → Cliente: BUNKER_STATE|4|100
Servidor → Cliente: LIVES|1|3
Servidor → Cliente: SCORE|1|0
Servidor → Cliente: ALIEN_CREATE|0|100|50|SQUID|10
Servidor → Cliente: ALIEN_CREATE|1|150|50|SQUID|10
... (más aliens)
```

### 5.2 Movimiento y disparo

```
Cliente → Servidor: MOVE|R
Servidor → Cliente: CANNON_POS|1|405
Cliente → Servidor: FIRE
Servidor → Cliente: BULLET_CREATE|10|405|550|PLAYER_1
Servidor → Cliente: BULLET_MOVE|10|405|540
Servidor → Cliente: BULLET_MOVE|10|405|530
...
Servidor → Cliente: ALIEN_KILL|3|1
Servidor → Cliente: BULLET_DESTROY|10
Servidor → Cliente: SCORE|1|20
```

### 5.3 Comando admin spawnea OVNI

```
[Admin escribe en consola del servidor:]
> OVNI I-D 1500

[Servidor procesa y envía a todos los clientes:]
Servidor → Cliente: UFO_CREATE|99|0|30|LR|1500
Servidor → Cliente: UFO_MOVE|99|10|30
...
```

## 6. Manejo de errores y desconexiones

### 6.1 Desconexión limpia
- Cliente envía `BYE\n`, servidor responde igual o simplemente cierra.
- Servidor elimina al cliente de la lista de observadores.

### 6.2 Desconexión abrupta (cable se desconecta, cliente crashea)
- Servidor detecta vía `IOException` en `read()` o `write()`.
- Servidor debe **NO crashear**, solo loggear el evento y limpiar recursos.
- Si era un jugador activo, su cañón puede:
  - Opción A: quedarse estático en pantalla (otros jugadores siguen jugando).
  - Opción B: eliminarse del juego.
  - Recomendado: Opción B + notificar a los demás con un mensaje informativo.

### 6.3 Mensajes mal formados
- Si llega un mensaje no parseable, loggear y descartar (no crashear).
- Responder con `ERROR|MALFORMED|<descripción>` opcionalmente.

### 6.4 Códigos de error sugeridos

| Código | Significado |
|--------|-------------|
| `MALFORMED` | Mensaje con formato inválido. |
| `SERVER_FULL` | No hay slots disponibles. |
| `INVALID_STATE` | Comando en momento incorrecto. |
| `UNKNOWN_COMMAND` | Comando no reconocido. |

## 7. Keep-alive

Para detectar desconexiones silenciosas:
- Cliente envía `PING\n` cada 5-10 segundos si no hay otro tráfico.
- Servidor responde con `PONG\n`.
- Si servidor o cliente no recibe nada en X segundos, considera la conexión muerta.

## 8. Concurrencia en el servidor

- **Un thread por cliente** para leer del socket (modelo simple y suficiente).
- **Un thread para el game loop** que actualiza el estado.
- Estado compartido (`GameState`) **debe estar sincronizado** (`synchronized`, `Lock`, `ConcurrentHashMap`, etc.).
- Patrón **Observer**: cuando el `GameState` cambia, notifica a todos los clientes conectados, que escriben al socket correspondiente.

## 9. Protocolo UART: Pico → Cliente C

El control físico usa un protocolo aún más simple por UART.

### 9.1 Configuración
- Baudrate: **115200**
- 8N1 (8 bits, sin paridad, 1 stop bit)
- Sin control de flujo

### 9.2 Mensajes (un byte ASCII por evento)

| Byte | Significado |
|------|-------------|
| `L` | Mover izquierda (1 toque del botón 1). |
| `R` | Mover derecha (2 toques del botón 1). |
| `F` | Disparar (botón 2). |

> **Razón:** mantener el protocolo UART simple. Un byte por evento, sin parsing. El cliente C los traduce a mensajes del protocolo del servidor.

### 9.3 Mapeo Pico → Servidor

```
Pico envía 'L' por UART
        ↓
Cliente C lee 'L'
        ↓
Cliente C envía "MOVE|L\n" al servidor por TCP
```

## 10. Tabla resumen de mensajes (referencia rápida)

```
Cliente → Servidor:
  HELLO|<type>|<name>
  MOVE|L | MOVE|R
  FIRE
  PING
  BYE

Servidor → Cliente:
  WELCOME|<id> | REJECT|<reason>
  FIELD|<w>|<h>
  GAME_START|<wave> | WAVE_START|<n> | GAME_OVER|<id>|<score> | GAME_END|<reason>
  ALIEN_CREATE|<id>|<x>|<y>|<type>|<pts>
  ALIEN_MOVE|<id>|<x>|<y>
  ALIEN_KILL|<id>|<killer>
  UFO_CREATE|<id>|<x>|<y>|<dir>|<base>
  UFO_MOVE|<id>|<x>|<y>
  UFO_KILL|<id>|<killer>|<pts>
  BULLET_CREATE|<id>|<x>|<y>|<owner>
  BULLET_MOVE|<id>|<x>|<y>
  BULLET_DESTROY|<id>
  BUNKER_STATE|<id>|<pct>
  CANNON_POS|<player>|<x>
  LIVES|<player>|<n>
  SCORE|<player>|<n>
  SPEED|<ms>
  PONG
  ERROR|<code>|<msg>

Admin → Servidor (consola):
  Crear (X, Y, Pts)
  OVNI I-D <pts> | OVNI D-I <pts>
  Velocidad <ms>
  Bunkers <pct>%
```
