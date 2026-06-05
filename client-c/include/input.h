#ifndef INPUT_H
#define INPUT_H

/*
 * input.h
 * -------
 * Lectura de teclado (y opcionalmente UART del Pico) para el cliente jugador.
 * Convierte el estado del teclado leido via raylib, o un evento del Pico, a
 * una de las acciones reconocidas por el protocolo.
 *
 * El loop principal llama input_leer_comando() una vez por frame y, si retorna
 * non-NULL, envia ese comando como INPUT al servidor. input_quiere_salir()
 * indica si el usuario pidio terminar (Esc o cerrar ventana).
 */

#include "pico.h"   /* para ConexionPico */

/**
 * Devuelve el comando de input segun el estado de teclado actual.
 * Posibles retornos: "MOVE_LEFT", "MOVE_RIGHT", "FIRE", "STOP", NULL.
 * NULL = no hay accion para enviar este frame.
 * Mapeo:
 *   - flecha izquierda o A: MOVE_LEFT
 *   - flecha derecha  o D : MOVE_RIGHT
 *   - barra espaciadora   : FIRE (solo en el frame de la pulsacion, no mantenido)
 *   - sin teclas relevantes: NULL
 */
const char *input_leer_comando(void);

/**
 * Variante que consulta primero al Pico por UART.
 * Si el Pico emitio un byte valido en este frame, retorna esa accion.
 * Si no, cae al input de teclado normal (input_leer_comando).
 * Si pico == NULL o no esta activo, equivale a input_leer_comando.
 */
const char *input_leer_comando_con_pico(ConexionPico *pico);

/** Retorna true (no cero) si el usuario presiono Esc o cerro la ventana. */
int input_quiere_salir(void);

/**
 * Retorna true (no cero) si el usuario presiono la barra espaciadora
 * EN ESTE FRAME. Pensado para la pantalla de inicio: el cliente la
 * llama en bucle y arranca el juego cuando devuelve true.
 */
int input_empezar_solicitado(void);

/**
 * Retorna true (no cero) si el usuario presiono la tecla R EN ESTE
 * FRAME. Se usa tras GAME_OVER: si esta activo, el cliente envia
 * un INPUT con accion {@code RESTART} para que el servidor reinicie
 * la partida.
 */
int input_reinicio_solicitado(void);

#endif /* INPUT_H */
