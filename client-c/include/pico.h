#ifndef PICO_H
#define PICO_H

/*
 * pico.h
 * ------
 * Conexion UART con el Raspberry Pi Pico que actua como control fisico del
 * juego. El Pico emite un byte ASCII por cada evento detectado:
 *   - 'L' = movimiento a la izquierda (un toque del boton 1)
 *   - 'R' = movimiento a la derecha   (dos toques rapidos del boton 1)
 *   - 'F' = disparo                   (boton 2)
 *
 * Este modulo abstrae la lectura no bloqueante del puerto serie y la
 * traduccion byte -> accion del protocolo del cliente.
 */

#include <stdbool.h>
#include "constants.h"

/**
 * Estado de la conexion UART con el Pico.
 * Se mantiene por valor en el caller; el fd se cierra con pico_cerrar.
 */
typedef struct {
    int fd;          /* file descriptor del puerto serie, -1 si cerrado */
    bool activo;     /* true si pico_abrir tuvo exito */
} ConexionPico;

/**
 * Inicializa la estructura a estado cerrado/inactivo.
 * Llamar antes de cualquier otra funcion del modulo.
 */
void pico_inicializar(ConexionPico *p);

/**
 * Abre el dispositivo serie en lectura no bloqueante, 115200 8N1, sin
 * paridad ni control de flujo. Si no puede abrir (no existe, permisos,
 * etc.), loguea el motivo a stderr, retorna false y deja la estructura
 * en estado inactivo (no crashea ni aborta el cliente).
 */
bool pico_abrir(ConexionPico *p, const char *dispositivo);

/**
 * Lee un byte del puerto si hay disponible.
 *  - Si hay byte, lo retorna.
 *  - Si no hay byte (EAGAIN/EWOULDBLOCK) o el dispositivo no esta activo,
 *    retorna 0.
 *  - Si detecta error fatal de lectura, marca activo = false, loguea y
 *    retorna 0.
 */
char pico_leer_byte(ConexionPico *p);

/**
 * Traduce un byte recibido a un comando del protocolo del cliente.
 * Retorna ACCION_IZQUIERDA, ACCION_DERECHA, ACCION_DISPARO o NULL si el
 * byte no corresponde a ningun mapeo conocido.
 */
const char *pico_byte_a_accion(char byte);

/**
 * Cierra el fd si estaba abierto y marca la conexion como inactiva.
 * Operacion idempotente.
 */
void pico_cerrar(ConexionPico *p);

#endif /* PICO_H */
