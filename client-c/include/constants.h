#ifndef CONSTANTS_H
#define CONSTANTS_H

/*
 * constants.h
 * -----------
 * Constantes globales del cliente C jugador/espectador.
 *
 * Toda magia numerica o cadena fija (tamanios, identificadores de tipo,
 * acciones reconocidas por el servidor, etc.) vive aqui. Ningun otro
 * .c/.h debe redefinir o hardcodear estos valores.
 */

/* ===== Ventana ===== */
#define VENTANA_ANCHO        800
#define VENTANA_ALTO         600
#define VENTANA_TITULO       "spaCEinvaders"
#define FPS                  60

/* ===== Red ===== */
#define SERVIDOR_IP_DEFAULT  "127.0.0.1"
#define SERVIDOR_PUERTO      5555
#define BUFFER_RED           16384       /* 16 KB para el STATE serializado */

/* ===== Limites de entidades ===== */
#define MAX_ALIENS_VISTA     128
#define MAX_BALAS_VISTA      64
#define MAX_BUNKERS_VISTA    8
#define MAX_CANONES_VISTA    4
#define MAX_JUGADORES_VISTA  4

/* ===== Tamanios fijos de id/string ===== */
#define ID_MAX               16
#define NOMBRE_MAX           32
#define TIPO_MAX             16
#define EVENTO_MAX           32

/* ===== Tipos de cliente ===== */
#define TIPO_PLAYER          "PLAYER"
#define TIPO_SPECTATOR       "SPECTATOR"

/* ===== Acciones de input ===== */
#define ACCION_IZQUIERDA     "MOVE_LEFT"
#define ACCION_DERECHA       "MOVE_RIGHT"
#define ACCION_DISPARO       "FIRE"
#define ACCION_QUIETO        "STOP"

/* ===== Colores (codigo por tipo) ===== */
/* Los valores raylib Color los maneja render.h; aqui solo identificadores semanticos. */

/* ===== UART (controlador fisico Pi Pico) ===== */
#define UART_DISPOSITIVO_DEFAULT  "/dev/ttyACM0"
#define UART_BAUDRATE_VALOR       B115200   /* macro de termios */
#define UART_BAUDRATE_DOC         115200    /* solo para logs */

/* Bytes ASCII que envia el Pico (uno por evento). */
#define PICO_BYTE_IZQUIERDA       'L'
#define PICO_BYTE_DERECHA         'R'
#define PICO_BYTE_DISPARO         'F'

#endif /* CONSTANTS_H */
