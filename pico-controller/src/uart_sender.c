#include "config.h"
#include "uart_sender.h"
#include "hardware/uart.h"
#include "hardware/gpio.h"

void uart_sender_init(void) {
    uart_init(UART_ID, UART_BAUDRATE);
    gpio_set_function(UART_TX_PIN, GPIO_FUNC_UART);
    gpio_set_function(UART_RX_PIN, GPIO_FUNC_UART);
    uart_set_format(UART_ID, 8, 1, UART_PARITY_NONE);  /* 8N1 */
    uart_set_hw_flow(UART_ID, false, false);           /* sin control de flujo */
}

void uart_enviar_byte(char b) {
    uart_putc_raw(UART_ID, b);
}
