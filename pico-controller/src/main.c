#include "pico/stdlib.h"
#include "config.h"
#include "button.h"
#include "uart_sender.h"

/*
 * Maquina de estados del boton 1 (movimiento):
 *   IDLE                  -> al detectar 1er toque pasa a ESPERANDO_SEGUNDO.
 *   ESPERANDO_SEGUNDO     -> si llega 2do toque dentro de VENTANA_DOBLE_TOQUE_MS
 *                            => envia 'R' (derecha).
 *                         -> si vence la ventana sin 2do toque
 *                            => envia 'L' (izquierda).
 * El boton 2 (disparo) es directo: cada flanco => 'F'.
 */
typedef enum {
    BOTON1_IDLE,
    BOTON1_ESPERANDO_SEGUNDO
} EstadoBoton1;

int main(void) {
    stdio_init_all();
    uart_sender_init();

    BotonDebounce b1, b2;
    boton_init(&b1, PIN_BOTON_MOVIMIENTO);
    boton_init(&b2, PIN_BOTON_DISPARO);

    EstadoBoton1 estado = BOTON1_IDLE;
    absolute_time_t tiempo_primer_toque = nil_time;

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

        sleep_ms(LOOP_DELAY_MS);
    }
    return 0;
}
