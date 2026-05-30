#ifndef INPUT_H
#define INPUT_H

/*
 * input.h
 * -------
 * Lectura de teclado para el cliente jugador. Convierte el estado del teclado
 * (leido via raylib) a una de las acciones reconocidas por el protocolo.
 *
 * El loop principal llama input_leer_comando() una vez por frame y, si retorna
 * non-NULL, envia ese comando como INPUT al servidor. input_quiere_salir()
 * indica si el usuario pidio terminar (Esc o cerrar ventana).
 */

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

/** Retorna true (no cero) si el usuario presiono Esc o cerro la ventana. */
int input_quiere_salir(void);

#endif /* INPUT_H */
