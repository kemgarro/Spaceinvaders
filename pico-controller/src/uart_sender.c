/*
 * uart_sender.c
 * -------------
 * Envia bytes hacia el cliente C. La transmision ocurre por USB-CDC
 * (puerto serial virtual sobre el conector USB del Pico): al enchufar el
 * Pico, en Linux aparece como /dev/ttyACM0 sin requerir adaptador externo.
 *
 * Usamos putchar_raw + stdio_flush para garantizar que cada byte salga
 * inmediatamente y sin traduccion de fin de linea (LF -> CRLF).
 */

#include "config.h"
#include "uart_sender.h"
#include "pico/stdlib.h"
#include "pico/stdio.h"

void uart_sender_init(void) {
    /* stdio_init_all() (en main.c) ya inicializa USB-CDC porque
     * pico_enable_stdio_usb(controller 1) esta declarado en CMakeLists.txt.
     * No hace falta init adicional aqui. */
}

void uart_enviar_byte(char b) {
    putchar_raw((int)b);
    stdio_flush();
}
