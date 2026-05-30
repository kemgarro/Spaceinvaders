#ifndef BUTTON_H
#define BUTTON_H

#include <stdbool.h>
#include "pico/stdlib.h"

/**
 * Estado interno de un boton con debounce.
 * Mantener uno por boton fisico.
 */
typedef struct {
    uint pin;                    /* numero de GPIO */
    bool ultimo_nivel;           /* ultimo nivel leido (true=alto=suelto, false=bajo=presionado) */
    absolute_time_t ultimo_cambio;
} BotonDebounce;

/** Inicializa el GPIO en modo entrada con pull-up interno. */
void boton_init(BotonDebounce *b, uint pin);

/**
 * Devuelve true si en esta lectura detectamos un FLANCO DE BAJADA estable
 * (i.e., el boton fue presionado y paso el debounce). Una sola vez por pulsacion.
 * Si el boton sigue presionado en llamadas posteriores, retorna false hasta que
 * sea soltado.
 */
bool boton_fue_presionado(BotonDebounce *b);

#endif
