/*
 * protocol.c
 * ----------
 * Implementacion del parser/serializador del protocolo del cliente.
 *
 * Usa cJSON (vendoreado en vendor/cjson) para parsear lineas JSON
 * recibidas desde el servidor y reconstruir un EstadoVista. Los mensajes
 * salientes (CONNECT, INPUT, DISCONNECT) se generan a mano con snprintf
 * porque son chicos y formato estable; evita allocaciones innecesarias.
 */

#include "protocol.h"

#include <stdio.h>
#include <string.h>
#include <stdlib.h>

#include "cJSON.h"

/* ===================================================================
 * Utilidades internas
 * =================================================================== */

/* Copia segura: trunca y siempre deja '\0' al final. */
static void copiar_str_seguro(char *dst, int dst_size, const char *src) {
    if (dst == NULL || dst_size <= 0) {
        return;
    }
    if (src == NULL) {
        dst[0] = '\0';
        return;
    }
    int i = 0;
    int limite = dst_size - 1;
    while (i < limite && src[i] != '\0') {
        dst[i] = src[i];
        i++;
    }
    dst[i] = '\0';
}

/* Lee un string de un objeto cJSON con seguridad; deja "" si no existe. */
static void leer_string(cJSON *obj, const char *clave, char *dst, int dst_size) {
    cJSON *item = cJSON_GetObjectItemCaseSensitive(obj, clave);
    if (cJSON_IsString(item) && item->valuestring != NULL) {
        copiar_str_seguro(dst, dst_size, item->valuestring);
    } else {
        if (dst != NULL && dst_size > 0) {
            dst[0] = '\0';
        }
    }
}

/* Lee un numero como int (trunca double). Default 0 si no existe. */
static int leer_int(cJSON *obj, const char *clave) {
    cJSON *item = cJSON_GetObjectItemCaseSensitive(obj, clave);
    if (cJSON_IsNumber(item)) {
        return (int)item->valuedouble;
    }
    return 0;
}

/* Lee un numero como long. Default 0 si no existe. */
static long leer_long(cJSON *obj, const char *clave) {
    cJSON *item = cJSON_GetObjectItemCaseSensitive(obj, clave);
    if (cJSON_IsNumber(item)) {
        return (long)item->valuedouble;
    }
    return 0L;
}

/* Lee un booleano. Default false si no existe. */
static bool leer_bool(cJSON *obj, const char *clave) {
    cJSON *item = cJSON_GetObjectItemCaseSensitive(obj, clave);
    if (cJSON_IsBool(item)) {
        return cJSON_IsTrue(item) ? true : false;
    }
    return false;
}

/* ===================================================================
 * Helpers de tipo
 * =================================================================== */

TipoVista protocolo_tipo_desde_str(const char *s) {
    if (s == NULL) {
        return TIPO_VISTA_DESCONOCIDO;
    }
    if (strcmp(s, "SQUID") == 0) {
        return TIPO_VISTA_SQUID;
    }
    if (strcmp(s, "CRAB") == 0) {
        return TIPO_VISTA_CRAB;
    }
    if (strcmp(s, "OCTOPUS") == 0) {
        return TIPO_VISTA_OCTOPUS;
    }
    if (strcmp(s, "OVNI") == 0) {
        return TIPO_VISTA_OVNI;
    }
    if (strcmp(s, "CANON") == 0) {
        return TIPO_VISTA_CANON;
    }
    if (strcmp(s, "BALA") == 0) {
        return TIPO_VISTA_BALA;
    }
    if (strcmp(s, "BUNKER") == 0) {
        return TIPO_VISTA_BUNKER;
    }
    return TIPO_VISTA_DESCONOCIDO;
}

const char *protocolo_tipo_a_str(TipoVista t) {
    switch (t) {
        case TIPO_VISTA_SQUID:   return "SQUID";
        case TIPO_VISTA_CRAB:    return "CRAB";
        case TIPO_VISTA_OCTOPUS: return "OCTOPUS";
        case TIPO_VISTA_OVNI:    return "OVNI";
        case TIPO_VISTA_CANON:   return "CANON";
        case TIPO_VISTA_BALA:    return "BALA";
        case TIPO_VISTA_BUNKER:  return "BUNKER";
        default:                 return "DESCONOCIDO";
    }
}

/* ===================================================================
 * Inicializacion
 * =================================================================== */

void protocolo_estado_inicializar(EstadoVista *estado) {
    if (estado == NULL) {
        return;
    }
    memset(estado, 0, sizeof(EstadoVista));
    estado->oleada = 0;
    estado->juego_terminado = false;
    estado->intervalo_aliens_ms = 0;
    estado->n_aliens = 0;
    estado->n_balas = 0;
    estado->n_bunkers = 0;
    estado->n_canones = 0;
    estado->n_jugadores = 0;
    estado->ovni_presente = false;
    estado->ultimo_evento[0] = '\0';
    estado->ultimo_evento_detalle[0] = '\0';
}

/* ===================================================================
 * Parseo de STATE
 * =================================================================== */

/* Parsea el array de aliens del campo data y lo vuelca al estado. */
static void parsear_aliens(EstadoVista *estado, cJSON *data) {
    cJSON *arr = cJSON_GetObjectItemCaseSensitive(data, "aliens");
    if (!cJSON_IsArray(arr)) {
        return;
    }
    cJSON *item = NULL;
    cJSON_ArrayForEach(item, arr) {
        if (estado->n_aliens >= MAX_ALIENS_VISTA) {
            fprintf(stderr, "protocolo: limite de aliens (%d) alcanzado, descartando resto\n",
                    MAX_ALIENS_VISTA);
            break;
        }
        EntidadVista *ev = &estado->aliens[estado->n_aliens];
        memset(ev, 0, sizeof(EntidadVista));
        leer_string(item, "id", ev->id, ID_MAX);
        char tipo_str[TIPO_MAX];
        leer_string(item, "tipo", tipo_str, TIPO_MAX);
        ev->tipo = protocolo_tipo_desde_str(tipo_str);
        ev->x = leer_int(item, "x");
        ev->y = leer_int(item, "y");
        ev->extra = leer_int(item, "puntos");
        copiar_str_seguro(ev->etiqueta, ID_MAX, tipo_str);
        estado->n_aliens++;
    }
}

/* Parsea el array de balas. */
static void parsear_balas(EstadoVista *estado, cJSON *data) {
    cJSON *arr = cJSON_GetObjectItemCaseSensitive(data, "balas");
    if (!cJSON_IsArray(arr)) {
        return;
    }
    cJSON *item = NULL;
    cJSON_ArrayForEach(item, arr) {
        if (estado->n_balas >= MAX_BALAS_VISTA) {
            fprintf(stderr, "protocolo: limite de balas (%d) alcanzado, descartando resto\n",
                    MAX_BALAS_VISTA);
            break;
        }
        EntidadVista *ev = &estado->balas[estado->n_balas];
        memset(ev, 0, sizeof(EntidadVista));
        leer_string(item, "id", ev->id, ID_MAX);
        ev->tipo = TIPO_VISTA_BALA;
        ev->x = leer_int(item, "x");
        ev->y = leer_int(item, "y");
        ev->extra = 0;
        /* Etiqueta: duenio (jugador id o "ALIEN") para que render decida color. */
        leer_string(item, "duenio", ev->etiqueta, ID_MAX);
        if (ev->etiqueta[0] == '\0') {
            /* Tolerancia: si llega "origen" (formato alternativo del doc), usarlo. */
            leer_string(item, "origen", ev->etiqueta, ID_MAX);
        }
        estado->n_balas++;
    }
}

/* Parsea el array de bunkers. */
static void parsear_bunkers(EstadoVista *estado, cJSON *data) {
    cJSON *arr = cJSON_GetObjectItemCaseSensitive(data, "bunkers");
    if (!cJSON_IsArray(arr)) {
        return;
    }
    cJSON *item = NULL;
    cJSON_ArrayForEach(item, arr) {
        if (estado->n_bunkers >= MAX_BUNKERS_VISTA) {
            fprintf(stderr, "protocolo: limite de bunkers (%d) alcanzado, descartando resto\n",
                    MAX_BUNKERS_VISTA);
            break;
        }
        EntidadVista *ev = &estado->bunkers[estado->n_bunkers];
        memset(ev, 0, sizeof(EntidadVista));
        leer_string(item, "id", ev->id, ID_MAX);
        ev->tipo = TIPO_VISTA_BUNKER;
        ev->x = leer_int(item, "x");
        ev->y = leer_int(item, "y");
        ev->extra = leer_int(item, "salud");
        /* Etiqueta opcional: estadoVisible (0..100) como cadena corta. */
        int visible = leer_int(item, "estadoVisible");
        snprintf(ev->etiqueta, ID_MAX, "%d", visible);
        estado->n_bunkers++;
    }
}

/* Parsea el array de canones. */
static void parsear_canones(EstadoVista *estado, cJSON *data) {
    cJSON *arr = cJSON_GetObjectItemCaseSensitive(data, "canones");
    if (!cJSON_IsArray(arr)) {
        return;
    }
    cJSON *item = NULL;
    cJSON_ArrayForEach(item, arr) {
        if (estado->n_canones >= MAX_CANONES_VISTA) {
            fprintf(stderr, "protocolo: limite de canones (%d) alcanzado, descartando resto\n",
                    MAX_CANONES_VISTA);
            break;
        }
        EntidadVista *ev = &estado->canones[estado->n_canones];
        memset(ev, 0, sizeof(EntidadVista));
        leer_string(item, "id", ev->id, ID_MAX);
        ev->tipo = TIPO_VISTA_CANON;
        ev->x = leer_int(item, "x");
        ev->y = leer_int(item, "y");
        ev->extra = 0;
        leer_string(item, "jugadorId", ev->etiqueta, ID_MAX);
        estado->n_canones++;
    }
}

/* Parsea el OVNI (objeto o null). */
static void parsear_ovni(EstadoVista *estado, cJSON *data) {
    cJSON *obj = cJSON_GetObjectItemCaseSensitive(data, "ovni");
    if (obj == NULL || cJSON_IsNull(obj) || !cJSON_IsObject(obj)) {
        estado->ovni_presente = false;
        return;
    }
    estado->ovni_presente = true;
    EntidadVista *ev = &estado->ovni;
    memset(ev, 0, sizeof(EntidadVista));
    leer_string(obj, "id", ev->id, ID_MAX);
    ev->tipo = TIPO_VISTA_OVNI;
    ev->x = leer_int(obj, "x");
    ev->y = leer_int(obj, "y");
    ev->extra = leer_int(obj, "puntosBase");
    leer_string(obj, "direccion", ev->etiqueta, ID_MAX);
}

/* Parsea el array de jugadores. */
static void parsear_jugadores(EstadoVista *estado, cJSON *data) {
    cJSON *arr = cJSON_GetObjectItemCaseSensitive(data, "jugadores");
    if (!cJSON_IsArray(arr)) {
        return;
    }
    cJSON *item = NULL;
    cJSON_ArrayForEach(item, arr) {
        if (estado->n_jugadores >= MAX_JUGADORES_VISTA) {
            fprintf(stderr, "protocolo: limite de jugadores (%d) alcanzado, descartando resto\n",
                    MAX_JUGADORES_VISTA);
            break;
        }
        JugadorVista *jv = &estado->jugadores[estado->n_jugadores];
        memset(jv, 0, sizeof(JugadorVista));
        leer_string(item, "id", jv->id, ID_MAX);
        leer_string(item, "nombre", jv->nombre, NOMBRE_MAX);
        jv->puntaje = leer_int(item, "puntaje");
        jv->vidas = leer_int(item, "vidas");
        estado->n_jugadores++;
    }
}

/* Aplica un STATE completo: resetea contadores y rellena. */
static void aplicar_state(EstadoVista *estado, cJSON *root) {
    cJSON *data = cJSON_GetObjectItemCaseSensitive(root, "data");
    if (!cJSON_IsObject(data)) {
        return;
    }
    /* metadatos */
    estado->oleada = leer_int(data, "oleada");
    estado->juego_terminado = leer_bool(data, "juegoTerminado");
    estado->intervalo_aliens_ms = leer_long(data, "intervaloAliensMs");

    /* reset de contadores antes de poblar */
    estado->n_aliens = 0;
    estado->n_balas = 0;
    estado->n_bunkers = 0;
    estado->n_canones = 0;
    estado->n_jugadores = 0;
    estado->ovni_presente = false;

    parsear_aliens(estado, data);
    parsear_balas(estado, data);
    parsear_bunkers(estado, data);
    parsear_canones(estado, data);
    parsear_ovni(estado, data);
    parsear_jugadores(estado, data);
}

/* Aplica un EVENT: actualiza ultimo_evento y arma una descripcion corta. */
static void aplicar_event(EstadoVista *estado, cJSON *root) {
    char nombre[EVENTO_MAX];
    leer_string(root, "name", nombre, EVENTO_MAX);
    if (nombre[0] == '\0') {
        copiar_str_seguro(estado->ultimo_evento, EVENTO_MAX, "EVENT");
    } else {
        copiar_str_seguro(estado->ultimo_evento, EVENTO_MAX, nombre);
    }

    /* Detalle: tomar algun campo conocido del payload si existe.
     * Limitamos tmp a un tamanio que deje espacio para prefijos como "alien=". */
    cJSON *payload = cJSON_GetObjectItemCaseSensitive(root, "payload");
    if (cJSON_IsObject(payload)) {
        char detalle[NOMBRE_MAX];
        detalle[0] = '\0';

        /* 8 bytes de margen para prefijos cortos (id=, alien=, bunker=, etc.). */
        char tmp[NOMBRE_MAX - 8];
        leer_string(payload, "id", tmp, (int)sizeof(tmp));
        if (tmp[0] != '\0') {
            snprintf(detalle, NOMBRE_MAX, "id=%s", tmp);
        }
        if (detalle[0] == '\0') {
            leer_string(payload, "alienId", tmp, (int)sizeof(tmp));
            if (tmp[0] != '\0') {
                snprintf(detalle, NOMBRE_MAX, "alien=%s", tmp);
            }
        }
        if (detalle[0] == '\0') {
            leer_string(payload, "bunkerId", tmp, (int)sizeof(tmp));
            if (tmp[0] != '\0') {
                snprintf(detalle, NOMBRE_MAX, "bunker=%s", tmp);
            }
        }
        if (detalle[0] == '\0') {
            int puntos = leer_int(payload, "puntos");
            if (puntos != 0) {
                snprintf(detalle, NOMBRE_MAX, "puntos=%d", puntos);
            }
        }
        copiar_str_seguro(estado->ultimo_evento_detalle, NOMBRE_MAX, detalle);
    } else {
        estado->ultimo_evento_detalle[0] = '\0';
    }
}

/* Aplica un ERROR: marca ultimo_evento = "ERROR" y guarda el texto. */
static void aplicar_error(EstadoVista *estado, cJSON *root) {
    copiar_str_seguro(estado->ultimo_evento, EVENTO_MAX, "ERROR");
    cJSON *payload = cJSON_GetObjectItemCaseSensitive(root, "payload");
    if (cJSON_IsObject(payload)) {
        char texto[NOMBRE_MAX];
        leer_string(payload, "error", texto, NOMBRE_MAX);
        if (texto[0] == '\0') {
            leer_string(payload, "message", texto, NOMBRE_MAX);
        }
        copiar_str_seguro(estado->ultimo_evento_detalle, NOMBRE_MAX, texto);
    } else {
        estado->ultimo_evento_detalle[0] = '\0';
    }
}

bool protocolo_aplicar_mensaje(EstadoVista *estado, const char *json) {
    if (estado == NULL || json == NULL) {
        return false;
    }
    cJSON *root = cJSON_Parse(json);
    if (root == NULL) {
        return false;
    }

    cJSON *tipo = cJSON_GetObjectItemCaseSensitive(root, "type");
    if (!cJSON_IsString(tipo) || tipo->valuestring == NULL) {
        cJSON_Delete(root);
        return false;
    }

    bool ok = true;
    if (strcmp(tipo->valuestring, "STATE") == 0) {
        aplicar_state(estado, root);
    } else if (strcmp(tipo->valuestring, "EVENT") == 0) {
        aplicar_event(estado, root);
    } else if (strcmp(tipo->valuestring, "ERROR") == 0) {
        aplicar_error(estado, root);
    } else {
        ok = false;
    }

    cJSON_Delete(root);
    return ok;
}

/* ===================================================================
 * Construccion de mensajes salientes
 * =================================================================== */

int protocolo_construir_connect(char *out, int max, const char *jugador_id, const char *tipo_cliente) {
    if (out == NULL || max <= 0 || jugador_id == NULL || tipo_cliente == NULL) {
        return -1;
    }
    int n = snprintf(out, (size_t)max,
                     "{\"type\":\"CONNECT\",\"id\":\"%s\",\"clientType\":\"%s\"}\n",
                     jugador_id, tipo_cliente);
    if (n < 0 || n >= max) {
        return -1;
    }
    return n;
}

int protocolo_construir_input(char *out, int max, const char *jugador_id, const char *accion) {
    if (out == NULL || max <= 0 || jugador_id == NULL || accion == NULL) {
        return -1;
    }
    int n = snprintf(out, (size_t)max,
                     "{\"type\":\"INPUT\",\"id\":\"%s\",\"action\":\"%s\"}\n",
                     jugador_id, accion);
    if (n < 0 || n >= max) {
        return -1;
    }
    return n;
}

int protocolo_construir_disconnect(char *out, int max, const char *jugador_id) {
    if (out == NULL || max <= 0 || jugador_id == NULL) {
        return -1;
    }
    int n = snprintf(out, (size_t)max,
                     "{\"type\":\"DISCONNECT\",\"id\":\"%s\"}\n",
                     jugador_id);
    if (n < 0 || n >= max) {
        return -1;
    }
    return n;
}
