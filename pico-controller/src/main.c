#include "pico/stdlib.h"
#include "config.h"
#include "button.h"
#include "uart_sender.h"

/* LED interno del Pico clasico: cableado a GP25, manejado como GPIO comun. */
#define PIN_LED_INTERNO 25

/*
 * Maquina de estados del boton 1 (movimiento):
 *   IDLE                  -> al detectar 1er toque pasa a ESPERANDO_SEGUNDO.
 *   ESPERANDO_SEGUNDO     -> si llega 2do toque dentro de VENTANA_DOBLE_TOQUE_MS
 *                            => envia 'R' (derecha).
 *                         -> si vence la ventana sin 2do toque
 *                            => envia 'L' (izquierda).
 * El boton 2 (disparo) es directo: cada flanco => 'F'.
 *
 * El LED interno (GP25 en el Pico clasico) parpadea a 1 Hz como indicador
 * de que el firmware esta vivo (heartbeat).
 */
typedef enum {
    BOTON1_IDLE,
    BOTON1_ESPERANDO_SEGUNDO
} EstadoBoton1;

/* Inicializa el LED interno (GP25). En el Pico clasico es un GPIO comun. */
static void led_init(void) {
    gpio_init(PIN_LED_INTERNO);
    gpio_set_dir(PIN_LED_INTERNO, GPIO_OUT);
    gpio_put(PIN_LED_INTERNO, 0);
}

/* Cambia el estado del LED interno. */
static void led_set(bool encendido) {
    gpio_put(PIN_LED_INTERNO, encendido ? 1 : 0);
}

int main(void) {
    stdio_init_all();
    uart_sender_init();
    led_init();

    BotonDebounce b1, b2;
    boton_init(&b1, PIN_BOTON_MOVIMIENTO);
    boton_init(&b2, PIN_BOTON_DISPARO);

    EstadoBoton1 estado = BOTON1_IDLE;
    absolute_time_t tiempo_primer_toque = nil_time;

    /* Heartbeat del LED: alterna cada LED_PERIODO_MS / 2 = 500 ms. */
    absolute_time_t tiempo_ultimo_toggle = get_absolute_time();
    bool led_encendido = false;

    while (true) {
        bool b1_press = boton_fue_presionado(&b1);
        bool b2_press = boton_fue_presionado(&b2);

        switch (estado) {
            case BOTON1_IDLE:
                if (b1_press) {
                    tiempo_primer_toque = get_absolute_time();
                    estado = BOTON1_ESPERANDO_SEGUNDO;
                }
                break;

            case BOTON1_ESPERANDO_SEGUNDO:
                if (b1_press) {
                    /* segundo toque dentro de la ventana => derecha */
                    uart_enviar_byte(BYTE_DERECHA);
                    estado = BOTON1_IDLE;
                } else {
                    int64_t transc = absolute_time_diff_us(tiempo_primer_toque, get_absolute_time());
                    if (transc >= (int64_t)VENTANA_DOBLE_TOQUE_MS * 1000) {
                        /* timeout: fue un solo toque => izquierda */
                        uart_enviar_byte(BYTE_IZQUIERDA);
                        estado = BOTON1_IDLE;
                    }
                }
                break;
        }

        if (b2_press) {
            uart_enviar_byte(BYTE_DISPARO);
        }

        /* Heartbeat del LED. No bloquea: solo alterna cuando paso medio periodo. */
        int64_t desde_toggle = absolute_time_diff_us(tiempo_ultimo_toggle, get_absolute_time());
        if (desde_toggle >= (int64_t)(LED_PERIODO_MS / 2) * 1000) {
            led_encendido = !led_encendido;
            led_set(led_encendido);
            tiempo_ultimo_toggle = get_absolute_time();
        }

        sleep_ms(LOOP_DELAY_MS);
    }
    return 0;
}
