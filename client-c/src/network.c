/*
 * network.c
 * ---------
 * Implementacion de la capa TCP no bloqueante del cliente.
 *
 * Estrategia:
 *   - connect() en modo bloqueante (mas simple y fiable para el handshake).
 *   - Tras connect, se setea O_NONBLOCK con fcntl para que el ciclo principal
 *     del juego no se bloquee leyendo del socket.
 *   - El recv acumula bytes en con->buffer; red_recibir_linea extrae lineas
 *     terminadas en '\n' una por una.
 *   - Si el buffer se llena sin encontrar '\n', se descarta (mensaje invalido).
 */

#include "network.h"

#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <unistd.h>
#include <errno.h>
#include <fcntl.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <sys/socket.h>
#include <sys/types.h>
#include <netdb.h>

/* ===================================================================
 * Inicializacion / cierre
 * =================================================================== */

void red_inicializar(Conexion *con) {
    if (con == NULL) {
        return;
    }
    con->fd = -1;
    con->pos = 0;
    con->conectado = false;
    /* el buffer no necesita inicializarse a 0; pos = 0 marca vacio. */
    con->buffer[0] = '\0';
}

void red_cerrar(Conexion *con) {
    if (con == NULL) {
        return;
    }
    if (con->fd >= 0) {
        close(con->fd);
        con->fd = -1;
    }
    con->pos = 0;
    con->conectado = false;
}

/* ===================================================================
 * Conexion
 * =================================================================== */

/* Setea el descriptor en modo no bloqueante. Retorna true si exito. */
static bool poner_no_bloqueante(int fd) {
    int flags = fcntl(fd, F_GETFL, 0);
    if (flags < 0) {
        return false;
    }
    if (fcntl(fd, F_SETFL, flags | O_NONBLOCK) < 0) {
        return false;
    }
    return true;
}

bool red_conectar(Conexion *con, const char *host, int puerto) {
    if (con == NULL || host == NULL) {
        return false;
    }
    red_inicializar(con);

    int fd = socket(AF_INET, SOCK_STREAM, 0);
    if (fd < 0) {
        fprintf(stderr, "red_conectar: socket() fallo: %s\n", strerror(errno));
        return false;
    }

    struct sockaddr_in dir;
    memset(&dir, 0, sizeof(dir));
    dir.sin_family = AF_INET;
    dir.sin_port = htons((uint16_t)puerto);

    /* Intentar parsear como IPv4 literal; si no, resolver con getaddrinfo. */
    if (inet_pton(AF_INET, host, &dir.sin_addr) != 1) {
        struct addrinfo hints;
        memset(&hints, 0, sizeof(hints));
        hints.ai_family = AF_INET;
        hints.ai_socktype = SOCK_STREAM;
        struct addrinfo *res = NULL;
        int rc = getaddrinfo(host, NULL, &hints, &res);
        if (rc != 0 || res == NULL) {
            fprintf(stderr, "red_conectar: no se pudo resolver host '%s'\n", host);
            close(fd);
            return false;
        }
        struct sockaddr_in *resuelta = (struct sockaddr_in *)res->ai_addr;
        dir.sin_addr = resuelta->sin_addr;
        freeaddrinfo(res);
    }

    if (connect(fd, (struct sockaddr *)&dir, sizeof(dir)) < 0) {
        fprintf(stderr, "red_conectar: connect() fallo a %s:%d: %s\n",
                host, puerto, strerror(errno));
        close(fd);
        return false;
    }

    /* Conexion establecida. Pasar a no bloqueante para el resto del ciclo. */
    if (!poner_no_bloqueante(fd)) {
        fprintf(stderr, "red_conectar: no se pudo poner en no bloqueante: %s\n",
                strerror(errno));
        close(fd);
        return false;
    }

    con->fd = fd;
    con->conectado = true;
    con->pos = 0;
    return true;
}

/* ===================================================================
 * Envio
 * =================================================================== */

bool red_enviar(Conexion *con, const char *datos, int largo) {
    if (con == NULL || datos == NULL || largo <= 0) {
        return false;
    }
    if (con->fd < 0 || !con->conectado) {
        return false;
    }

    int enviados = 0;
    /* Loop por si el socket no bloqueante acepta parcialmente. */
    while (enviados < largo) {
        ssize_t n = send(con->fd, datos + enviados, (size_t)(largo - enviados), 0);
        if (n > 0) {
            enviados += (int)n;
            continue;
        }
        if (n < 0 && (errno == EAGAIN || errno == EWOULDBLOCK || errno == EINTR)) {
            /* Reintentar despues de pequena espera para no spinear. */
            usleep(RETRY_RED_USLEEP);
            continue;
        }
        fprintf(stderr, "red_enviar: send() fallo: %s\n", strerror(errno));
        con->conectado = false;
        return false;
    }
    return true;
}

/* ===================================================================
 * Recepcion line-delimited
 * =================================================================== */

/* Busca un '\n' en con->buffer[0..pos]. Si lo encuentra, copia la linea
 * (sin '\n') a linea_out, desplaza el resto al inicio del buffer y retorna
 * true. Si no, retorna false sin tocar el buffer. */
static bool extraer_linea_buffer(Conexion *con, char *linea_out, int max) {
    int i;
    for (i = 0; i < con->pos; i++) {
        if (con->buffer[i] == '\n') {
            int largo = i;
            int copia = (largo < max - 1) ? largo : (max - 1);
            if (copia < 0) {
                copia = 0;
            }
            memcpy(linea_out, con->buffer, (size_t)copia);
            linea_out[copia] = '\0';

            /* Desplazar lo que sigue al inicio del buffer. */
            int restante = con->pos - (i + 1);
            if (restante > 0) {
                memmove(con->buffer, con->buffer + i + 1, (size_t)restante);
            }
            con->pos = restante;
            return true;
        }
    }
    return false;
}

bool red_recibir_linea(Conexion *con, char *linea_out, int max) {
    if (con == NULL || linea_out == NULL || max <= 1) {
        return false;
    }
    if (con->fd < 0 || !con->conectado) {
        return false;
    }

    /* 1) Si ya hay una linea completa en el buffer, devolverla. */
    if (extraer_linea_buffer(con, linea_out, max)) {
        return true;
    }

    /* 2) Intentar leer mas del socket. */
    if (con->pos >= BUFFER_RED - 1) {
        /* Buffer lleno sin '\n': mensaje corrupto. Descartar y avisar. */
        fprintf(stderr, "red_recibir_linea: buffer lleno sin '\\n', descartando %d bytes\n",
                con->pos);
        con->pos = 0;
    }

    ssize_t n = recv(con->fd, con->buffer + con->pos,
                     (size_t)(BUFFER_RED - 1 - con->pos), 0);
    if (n > 0) {
        con->pos += (int)n;
        /* Intentar de nuevo extraer; puede que ahora si haya '\n'. */
        if (extraer_linea_buffer(con, linea_out, max)) {
            return true;
        }
        return false;
    }
    if (n == 0) {
        /* Conexion cerrada por el servidor. */
        fprintf(stderr, "red_recibir_linea: servidor cerro la conexion\n");
        con->conectado = false;
        return false;
    }
    /* n < 0 */
    if (errno == EAGAIN || errno == EWOULDBLOCK || errno == EINTR) {
        return false; /* sin datos por ahora, no es error */
    }
    fprintf(stderr, "red_recibir_linea: recv() fallo: %s\n", strerror(errno));
    con->conectado = false;
    return false;
}
