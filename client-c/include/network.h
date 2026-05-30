#ifndef NETWORK_H
#define NETWORK_H

#include <stdbool.h>
#include "constants.h"

/*
 * network.h
 * ---------
 * Capa TCP no bloqueante para hablar con el servidor Java.
 *
 * El cliente abre una unica conexion al servidor, envia mensajes cortos
 * (CONNECT/INPUT/DISCONNECT) y recibe mensajes JSON line-delimited
 * (STATE/EVENT/ERROR). La estructura Conexion mantiene el socket y un
 * buffer de acumulacion para extraer lineas completas.
 */

typedef struct {
    int fd;                          /* socket file descriptor, -1 si cerrado */
    char buffer[BUFFER_RED];         /* buffer de recepcion para line-delimited */
    int pos;                         /* bytes acumulados en buffer */
    bool conectado;
} Conexion;

/** Inicializa la estructura a estado cerrado. Llamar antes de cualquier otra. */
void red_inicializar(Conexion *con);

/**
 * Abre TCP a host:puerto. Setea socket en NO BLOQUEANTE.
 * Retorna true si conecto, false si fallo (DNS, connect refused, etc.).
 */
bool red_conectar(Conexion *con, const char *host, int puerto);

/**
 * Envia datos completos al servidor (bloquea hasta enviar todo o error).
 * Para mensajes cortos como CONNECT/INPUT esto es suficiente.
 * Retorna true si envio todo, false en error.
 */
bool red_enviar(Conexion *con, const char *datos, int largo);

/**
 * Intenta extraer UNA linea completa (terminada en \n) del buffer.
 * Si hay una linea: copia a linea_out (sin el \n, terminada en \0), desplaza buffer, retorna true.
 * Si no hay linea completa: hace recv NO BLOQUEANTE para acumular mas datos en el buffer.
 *   Si llegaron datos pero aun no hay \n, retorna false (no hay linea aun).
 *   Si el socket se cerro / error fatal, marca con->conectado = false y retorna false.
 */
bool red_recibir_linea(Conexion *con, char *linea_out, int max);

/** Cierra el socket si esta abierto. Idempotente. */
void red_cerrar(Conexion *con);

#endif /* NETWORK_H */
