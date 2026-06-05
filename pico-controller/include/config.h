#ifndef CONFIG_H
#define CONFIG_H

#include "pico/stdlib.h"

/* ===== Pines GPIO ===== */
#define PIN_BOTON_MOVIMIENTO     2     /* boton 1: 1 toque = izq, 2 toques = der */
#define PIN_BOTON_DISPARO        3     /* boton 2: disparo */

/* ===== UART ===== */
#define UART_ID                  uart0
#define UART_TX_PIN              0
#define UART_RX_PIN              1     /* no usado pero declarado por convencion */
#define UART_BAUDRATE            115200

/* ===== Tiempos ===== */
#define VENTANA_DOBLE_TOQUE_MS   300   /* 1 vs 2 toques del boton 1 */
#define DEBOUNCE_MS              20    /* debounce minimo del flanco */
#define LOOP_DELAY_MS            1     /* pequena pausa entre iteraciones */
#define LED_PERIODO_MS           1000  /* heartbeat del LED interno (1 Hz) */

/* ===== Bytes que se envian por UART ===== */
#define BYTE_IZQUIERDA           'L'
#define BYTE_DERECHA             'R'
#define BYTE_DISPARO             'F'

#endif
