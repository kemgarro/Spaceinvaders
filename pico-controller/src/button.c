#include "button.h"
#include "config.h"

void boton_init(BotonDebounce *b, uint pin) {
    b->pin = pin;
    gpio_init(pin);
    gpio_set_dir(pin, GPIO_IN);
    gpio_pull_up(pin);            /* boton a GND; presionado = lectura 0 */
    b->ultimo_nivel = true;       /* arranco en "suelto" */
    b->ultimo_cambio = get_absolute_time();
}

bool boton_fue_presionado(BotonDebounce *b) {
    bool actual = gpio_get(b->pin);
    if (actual == b->ultimo_nivel) return false;

    int64_t transcurrido_us = absolute_time_diff_us(b->ultimo_cambio, get_absolute_time());
    if (transcurrido_us < (int64_t)DEBOUNCE_MS * 1000) return false;

    b->ultimo_nivel = actual;
    b->ultimo_cambio = get_absolute_time();
    /* Flanco de bajada = presionado. */
    return (actual == false);
}
