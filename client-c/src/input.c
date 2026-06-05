/*
 * input.c
 * -------
 * Mapeo de teclado a comandos del protocolo. Solo depende de raylib para
 * IsKeyDown/IsKeyPressed/WindowShouldClose.
 *
 * Politica:
 *  - FIRE tiene prioridad y solo se emite en el frame en que se PRESIONA espacio
 *    (no se mantiene). Asi un mantenido del jugador no satura al servidor con
 *    disparos repetidos.
 *  - Movimiento se reporta cada N frames mientras la tecla este abajo, para no
 *    saturar la red con MOVE_LEFT/MOVE_RIGHT a 60 Hz.
 *  - Si no hay tecla relevante, se retorna NULL (no se envia nada).
 */

#include <stddef.h>

#include "raylib.h"

#include "constants.h"
#include "input.h"
#include "pico.h"

const char *input_leer_comando(void) {
    /* FIRE prioritario: solo en el frame de la pulsacion. */
    if (IsKeyPressed(KEY_SPACE)) {
        return ACCION_DISPARO;
    }

    bool izquierda = IsKeyDown(KEY_LEFT) || IsKeyDown(KEY_A);
    bool derecha   = IsKeyDown(KEY_RIGHT) || IsKeyDown(KEY_D);

    /* Throttle: contamos frames y solo emitimos uno de cada N. */
    static int contador = 0;
    contador++;

    if (izquierda && !derecha) {
        if (contador % INPUT_THROTTLE_MOVIMIENTO == 0) {
            return ACCION_IZQUIERDA;
        }
        return NULL;
    }
    if (derecha && !izquierda) {
        if (contador % INPUT_THROTTLE_MOVIMIENTO == 0) {
            return ACCION_DERECHA;
        }
        return NULL;
    }

    return NULL;
}

const char *input_leer_comando_con_pico(ConexionPico *pico) {
    /* 1. Consultar al Pico primero: el control fisico tiene prioridad sobre
     *    el teclado. Asi, si el jugador esta usando el Pico, no le pisamos
     *    su accion con una tecla suelta. */
    if (pico != NULL && pico->activo) {
        char b = pico_leer_byte(pico);
        if (b != 0) {
            const char *accion = pico_byte_a_accion(b);
            if (accion != NULL) {
                return accion;
            }
        }
    }
    /* 2. Fallback al teclado. */
    return input_leer_comando();
}

int input_quiere_salir(void) {
    return (WindowShouldClose() || IsKeyPressed(KEY_ESCAPE)) ? 1 : 0;
}

int input_empezar_solicitado(void) {
    return IsKeyPressed(KEY_SPACE) ? 1 : 0;
}

int input_reinicio_solicitado(void) {
    return IsKeyPressed(KEY_R) ? 1 : 0;
}
