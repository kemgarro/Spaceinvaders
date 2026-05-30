/*
 * pico.c
 * ------
 * Implementacion del modulo de comunicacion UART con el Raspberry Pi Pico.
 *
 * El puerto serie se abre en modo no bloqueante y configurado como raw
 * (sin canonical mode, sin echo, sin procesamiento de control). El loop
 * principal del cliente llama a pico_leer_byte() una vez por frame; si
 * no hay datos, la llamada retorna inmediatamente sin bloquear.
 *
 * El protocolo del Pico es muy simple: 1 byte ASCII por evento, sin
 * terminadores. Por eso aqui no hay buffering ni parser de lineas; cada
 * byte recibido se traduce directamente a una accion del protocolo.
 */

#include <errno.h>
#include <fcntl.h>
#include <stdbool.h>
#include <stdio.h>
#include <string.h>
#include <sys/types.h>
#include <termios.h>
#include <unistd.h>

#include "constants.h"
#include "pico.h"

void pico_inicializar(ConexionPico *p) {
    if (p == NULL) return;
    p->fd = -1;
    p->activo = false;
}

bool pico_abrir(ConexionPico *p, const char *dispositivo) {
    if (p == NULL || dispositivo == NULL) {
        return false;
    }

    /* Abrimos en RDWR (algunos drivers lo exigen aunque solo leamos),
     * NOCTTY para que el tty no se vuelva nuestro controlador, y
     * NONBLOCK para que las lecturas posteriores no bloqueen. */
    int fd = open(dispositivo, O_RDWR | O_NOCTTY | O_NONBLOCK);
    if (fd < 0) {
        fprintf(stderr, "pico: no se pudo abrir %s: %s\n",
                dispositivo, strerror(errno));
        p->fd = -1;
        p->activo = false;
        return false;
    }

    struct termios tty;
    memset(&tty, 0, sizeof(tty));
    if (tcgetattr(fd, &tty) != 0) {
        fprintf(stderr, "pico: tcgetattr fallo en %s: %s\n",
                dispositivo, strerror(errno));
        close(fd);
        p->fd = -1;
        p->activo = false;
        return false;
    }

    /* Baudios in/out: 115200. */
    cfsetispeed(&tty, UART_BAUDRATE_VALOR);
    cfsetospeed(&tty, UART_BAUDRATE_VALOR);

    /* 8 bits de datos. */
    tty.c_cflag = (tty.c_cflag & ~CSIZE) | CS8;
    /* Sin paridad. */
    tty.c_cflag &= ~PARENB;
    /* 1 stop bit. */
    tty.c_cflag &= ~CSTOPB;
    /* Ignorar lineas de control modem + habilitar lectura. */
    tty.c_cflag |= (CLOCAL | CREAD);
    /* Sin control de flujo por hardware (no todos los headers lo definen). */
#ifdef CRTSCTS
    tty.c_cflag &= ~CRTSCTS;
#endif

    /* Modo raw: sin canonical, sin echo, sin senales generadas por el tty. */
    tty.c_lflag = 0;
    /* Ignorar break; sin software flow control ni traduccion CR/LF. */
    tty.c_iflag = IGNBRK;
    /* Sin procesamiento de salida. */
    tty.c_oflag = 0;

    /* Lectura no bloqueante: regresa apenas haya algo (o nada). */
    tty.c_cc[VMIN]  = 0;
    tty.c_cc[VTIME] = 0;

    if (tcsetattr(fd, TCSANOW, &tty) != 0) {
        fprintf(stderr, "pico: tcsetattr fallo en %s: %s\n",
                dispositivo, strerror(errno));
        close(fd);
        p->fd = -1;
        p->activo = false;
        return false;
    }

    p->fd = fd;
    p->activo = true;
    return true;
}

char pico_leer_byte(ConexionPico *p) {
    if (p == NULL || !p->activo || p->fd < 0) {
        return 0;
    }

    char c;
    ssize_t n = read(p->fd, &c, 1);
    if (n == 1) {
        return c;
    }
    if (n == 0) {
        /* EOF: el otro extremo cerro. Lo tratamos como "no hay datos"
         * este frame; no marcamos inactivo porque algunos drivers de pty
         * reportan 0 transitoriamente. */
        return 0;
    }
    if (n < 0 && (errno == EAGAIN || errno == EWOULDBLOCK)) {
        return 0;
    }

    /* Error fatal: dejamos de leer pero no abortamos el cliente. */
    fprintf(stderr, "pico: error de lectura: %s\n", strerror(errno));
    p->activo = false;
    return 0;
}

const char *pico_byte_a_accion(char byte) {
    switch (byte) {
        case PICO_BYTE_IZQUIERDA: return ACCION_IZQUIERDA;
        case PICO_BYTE_DERECHA:   return ACCION_DERECHA;
        case PICO_BYTE_DISPARO:   return ACCION_DISPARO;
        default:                  return NULL;
    }
}

void pico_cerrar(ConexionPico *p) {
    if (p == NULL) return;
    if (p->fd >= 0) {
        close(p->fd);
    }
    p->fd = -1;
    p->activo = false;
}
