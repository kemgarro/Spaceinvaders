#ifndef PROTOCOL_H
#define PROTOCOL_H

#include <stdbool.h>
#include "constants.h"

/*
 * protocol.h
 * ----------
 * API de parseo y construccion de mensajes JSON del protocolo spaCEinvaders.
 *
 * El cliente solo "ve" el mundo a traves de un EstadoVista, un snapshot
 * plano de las entidades activas en el ultimo tick recibido. Los mensajes
 * STATE reescriben este snapshot completo; los mensajes EVENT actualizan
 * el ultimo evento conocido para HUD/audio.
 */

/* ===== Tipos de entidad (string-keyed para fidelidad al servidor) ===== */
typedef enum {
    TIPO_VISTA_DESCONOCIDO = 0,
    TIPO_VISTA_SQUID,
    TIPO_VISTA_CRAB,
    TIPO_VISTA_OCTOPUS,
    TIPO_VISTA_OVNI,
    TIPO_VISTA_CANON,
    TIPO_VISTA_BALA,
    TIPO_VISTA_BUNKER
} TipoVista;

/* Una entidad simple para render (posicion + categoria). */
typedef struct {
    char id[ID_MAX];
    TipoVista tipo;
    int x;
    int y;
    int extra;             /* puntos / salud / etc segun tipo */
    char etiqueta[ID_MAX]; /* dueno (bala), jugadorId (canon), direccion OVNI/bala como texto, ... */
} EntidadVista;

typedef struct {
    char id[ID_MAX];
    char nombre[NOMBRE_MAX];
    int puntaje;
    int vidas;
} JugadorVista;

/* Snapshot completo del estado del juego para el cliente. */
typedef struct {
    /* metadata */
    int oleada;
    bool juego_terminado;
    long intervalo_aliens_ms;

    /* listas (arrays de tamanio fijo + contador real) */
    EntidadVista aliens[MAX_ALIENS_VISTA];
    int n_aliens;

    EntidadVista balas[MAX_BALAS_VISTA];
    int n_balas;

    EntidadVista bunkers[MAX_BUNKERS_VISTA];
    int n_bunkers;

    EntidadVista canones[MAX_CANONES_VISTA];
    int n_canones;

    JugadorVista jugadores[MAX_JUGADORES_VISTA];
    int n_jugadores;

    /* OVNI: hay uno o ninguno */
    bool ovni_presente;
    EntidadVista ovni;

    /* ultimo evento (para HUD): nombre + descripcion corta */
    char ultimo_evento[EVENTO_MAX];
    char ultimo_evento_detalle[NOMBRE_MAX];
} EstadoVista;

/* ===== Funciones ===== */

/** Inicializa un EstadoVista a vacio (todos los contadores en 0). */
void protocolo_estado_inicializar(EstadoVista *estado);

/**
 * Aplica una linea JSON recibida del servidor al estado.
 * Soporta type=STATE (reescribe el snapshot completo), type=EVENT (actualiza ultimo_evento),
 * type=ERROR (idem ultimo_evento con "ERROR" como nombre).
 * Retorna true si el mensaje se aplico correctamente, false si JSON invalido o type no soportado.
 */
bool protocolo_aplicar_mensaje(EstadoVista *estado, const char *json);

/**
 * Construye una linea JSON CONNECT lista para enviar (incluye \n al final).
 * jugador_id: id que se usara para identificar al cliente.
 * tipo_cliente: TIPO_PLAYER o TIPO_SPECTATOR.
 * target: id del jugador a observar (obligatorio para SPECTATOR). Si es NULL o
 *         cadena vacia, no se incluye el campo en el JSON (caso PLAYER).
 * Retorna la cantidad de bytes escritos en out, o -1 si overflow.
 */
int protocolo_construir_connect(char *out, int max,
                                const char *jugador_id,
                                const char *tipo_cliente,
                                const char *target);

/**
 * Construye una linea JSON INPUT lista para enviar (incluye \n al final).
 * accion: ACCION_IZQUIERDA, ACCION_DERECHA, ACCION_DISPARO, ACCION_QUIETO.
 */
int protocolo_construir_input(char *out, int max, const char *jugador_id, const char *accion);

/** Construye una linea JSON DISCONNECT. */
int protocolo_construir_disconnect(char *out, int max, const char *jugador_id);

/** Helper: convierte string "SQUID"/"CRAB"/... a TipoVista. */
TipoVista protocolo_tipo_desde_str(const char *s);

/** Helper: convierte TipoVista a string para debug. */
const char *protocolo_tipo_a_str(TipoVista t);

#endif /* PROTOCOL_H */
