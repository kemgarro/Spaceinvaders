/*
 * main_blink_test.c
 * -----------------
 * Firmware de DIAGNOSTICO minimo. No hace nada mas que parpadear el LED
 * interno (GP25) a 2 Hz. No inicializa USB, no toca botones, no toca UART.
 *
 * Objetivo: si este firmware tampoco hace parpadear el LED, sabemos que el
 * problema no es nuestro codigo sino el flasheo o el hardware del Pico.
 * Si parpadea, sabemos que el GPIO 25 funciona y volvemos a depurar el
 * firmware real.
 *
 * Para usar: temporalmente cambiar add_executable() en CMakeLists.txt
 * para que apunte aqui en vez de src/main.c, recompilar, flashear.
 */

#include "pico/stdlib.h"

/* Pines candidatos donde podria estar el LED de board en distintos modelos:
 *   GP25 -> Raspberry Pi Pico clasico oficial
 *   GP16 -> Waveshare RP2040-Zero (LED RGB WS2812 - no se enciende solo con GPIO HIGH)
 *   GP23 -> YD-RP2040 (LED RGB)
 *   GP24 -> algunos clones
 * Si alguno se enciende como LED comun, ese es el correcto.
 */
static const uint pines_candidatos[] = {16, 23, 24, 25};
static const uint cantidad_pines = sizeof(pines_candidatos) / sizeof(pines_candidatos[0]);

int main(void) {
    for (uint i = 0; i < cantidad_pines; ++i) {
        gpio_init(pines_candidatos[i]);
        gpio_set_dir(pines_candidatos[i], GPIO_OUT);
        gpio_put(pines_candidatos[i], 0);
    }

    while (true) {
        for (uint i = 0; i < cantidad_pines; ++i) {
            gpio_put(pines_candidatos[i], 1);
        }
        sleep_ms(250);
        for (uint i = 0; i < cantidad_pines; ++i) {
            gpio_put(pines_candidatos[i], 0);
        }
        sleep_ms(250);
    }
}
