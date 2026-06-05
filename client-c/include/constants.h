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
#define ACCION_REINICIAR     "RESTART"

/* ===== Throttling de input =====
 * Mover el canon en cada frame inunda al servidor; lo enviamos cada N
 * frames mientras la tecla se mantenga presionada. */
#define INPUT_THROTTLE_MOVIMIENTO  3

/* ===== Defaults del entry point del cliente ===== */
#define CLIENTE_ID_DEFAULT         "p1"
#define CLIENTE_BUFFER_MENSAJE     256

/* ===== Etiquetas del protocolo del servidor ===== */
/* El servidor marca las balas alien con esta etiqueta en el campo "duenio". */
#define PROTOCOLO_ETIQUETA_BALA_ALIEN  "ALIEN"

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

/* ===== Constantes de presentacion (alpha / transparencia) =====
 * Valores de canal alpha (0..255) para los distintos estados visuales
 * de los bunkers y para el overlay de GAME OVER. Centralizados aqui
 * para evitar numeros magicos en render.c. */
#define ALPHA_BUNKER_INTACTO      255           /* salud alta: opaco */
#define ALPHA_BUNKER_DANADO       (255 * 60 / 100)  /* salud media: ~60% */
#define ALPHA_BUNKER_CRITICO      (255 * 30 / 100)  /* salud baja: ~30% */
#define ALPHA_OVERLAY_GAMEOVER    180           /* fondo semitransparente del GAME OVER */

/* ===== Constantes de red ===== */
/* Microsegundos a esperar entre reintentos cuando send() en socket
 * no bloqueante devuelve EAGAIN/EWOULDBLOCK/EINTR, para no spinear. */
#define RETRY_RED_USLEEP          1000

#endif /* CONSTANTS_H */
