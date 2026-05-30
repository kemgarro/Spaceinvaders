#ifndef UART_SENDER_H
#define UART_SENDER_H

/** Inicializa UART0 a 115200 8N1 sin control de flujo en los pines GP0/GP1. */
void uart_sender_init(void);

/** Envia un byte crudo (sin terminadores) por el UART configurado. */
void uart_enviar_byte(char b);

#endif
