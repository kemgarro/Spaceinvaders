/*
 * render.c
 * --------
 * Implementacion del modulo de render con raylib. Se evita exponer Color o
 * cualquier tipo de raylib hacia los headers para no acoplar el resto del
 * cliente a la libreria grafica.
 */

#include <stdio.h>
#include <string.h>

#include "raylib.h"

#include "constants.h"
#include "protocol.h"
#include "render.h"

/* ===== Etiqueta que el servidor usa para balas disparadas por aliens ===== */
#define RENDER_ETIQUETA_BALA_ALIEN "ALIEN"

/* ===== Helpers privados al modulo ===== */

/* Devuelve el color base de un alien segun su tipo. OVNI tiene su propio caso. */
static Color render_color_alien(TipoVista tipo) {
    switch (tipo) {
        case TIPO_VISTA_SQUID:   return WHITE;
        case TIPO_VISTA_CRAB:    return SKYBLUE;
        case TIPO_VISTA_OCTOPUS: return MAGENTA;
        case TIPO_VISTA_OVNI:    return RED;
        default:                 return GRAY;
    }
}

/* Busca un jugador por id en el estado; retorna NULL si no esta. */
static const JugadorVista *render_buscar_jugador(const EstadoVista *estado, const char *id) {
    if (estado == NULL || id == NULL) {
        return NULL;
    }
    for (int i = 0; i < estado->n_jugadores; i++) {
        if (strncmp(estado->jugadores[i].id, id, ID_MAX) == 0) {
            return &estado->jugadores[i];
        }
    }
    return NULL;
}

/* Dibuja el HUD superior con puntaje/vidas/oleada del jugador local. */
static void render_dibujar_hud(const EstadoVista *estado, const char *id_jugador_local) {
    char texto[128];
    const JugadorVista *yo = render_buscar_jugador(estado, id_jugador_local);

    if (yo != NULL) {
        snprintf(texto, sizeof(texto), "Puntaje: %d  Vidas: %d  Oleada: %d",
                 yo->puntaje, yo->vidas, estado->oleada);
    } else {
        snprintf(texto, sizeof(texto), "esperando estado...");
    }

    DrawText(texto, RENDER_HUD_MARGEN, RENDER_HUD_MARGEN, RENDER_HUD_FONT_SIZE, RAYWHITE);
}

/* Dibuja el HUD del espectador (encabezado y stats del jugador observado). */
static void render_dibujar_hud_espectador(const EstadoVista *estado, const char *target) {
    char texto[160];
    if (target == NULL || target[0] == '\0') {
        snprintf(texto, sizeof(texto), "MODO ESPECTADOR (sin target)");
        DrawText(texto, RENDER_HUD_MARGEN, RENDER_HUD_MARGEN, RENDER_HUD_FONT_SIZE, RED);
        return;
    }
    const JugadorVista *obs = render_buscar_jugador(estado, target);
    if (obs == NULL) {
        snprintf(texto, sizeof(texto), "JUGADOR %s DESCONECTADO", target);
        DrawText(texto, RENDER_HUD_MARGEN, RENDER_HUD_MARGEN, RENDER_HUD_FONT_SIZE, RED);
        return;
    }
    snprintf(texto, sizeof(texto), "OBSERVANDO A %s", target);
    DrawText(texto, RENDER_HUD_MARGEN, RENDER_HUD_MARGEN, RENDER_HUD_FONT_SIZE, LIME);
    char stats[128];
    snprintf(stats, sizeof(stats), "Puntaje: %d  Vidas: %d  Oleada: %d",
             obs->puntaje, obs->vidas, estado->oleada);
    DrawText(stats,
             RENDER_HUD_MARGEN,
             RENDER_HUD_MARGEN + RENDER_HUD_FONT_SIZE + 4,
             RENDER_HUD_FONT_SIZE,
             RAYWHITE);
}

/* Dibuja una columna a la derecha con todos los jugadores conectados. El
 * parametro id_destacado marca con un puntero al jugador local (modo PLAYER)
 * o al jugador observado (modo SPECTATOR). */
static void render_dibujar_sidebar(const EstadoVista *estado, const char *id_destacado) {
    if (estado == NULL || estado->n_jugadores == 0) {
        return;
    }
    int x = VENTANA_ANCHO - RENDER_SIDEBAR_ANCHO - RENDER_SIDEBAR_MARGEN_DER;
    int y = RENDER_SIDEBAR_TITULO_Y;

    DrawText("JUGADORES", x, y, RENDER_SIDEBAR_FONT_SIZE, RAYWHITE);
    DrawText("-----------", x, y + RENDER_SIDEBAR_INTERLINEA,
             RENDER_SIDEBAR_FONT_SIZE, GRAY);

    int fila_y = y + (RENDER_SIDEBAR_INTERLINEA * 2);
    char texto[64];
    for (int i = 0; i < estado->n_jugadores; i++) {
        const JugadorVista *j = &estado->jugadores[i];
        bool destacado = (id_destacado != NULL &&
                          strncmp(j->id, id_destacado, ID_MAX) == 0);
        const char *marcador = destacado ? "> " : "  ";
        /* Formato: marcador + id (5 cols) + puntaje (4) + V vidas */
        snprintf(texto, sizeof(texto), "%s%-5s %4d  V%d",
                 marcador, j->id, j->puntaje, j->vidas);
        Color color = destacado ? LIME : RAYWHITE;
        DrawText(texto, x, fila_y, RENDER_SIDEBAR_FONT_SIZE, color);
        fila_y += RENDER_SIDEBAR_INTERLINEA;
    }
}

/* Dibuja el ultimo evento recibido en la esquina inferior derecha. */
static void render_dibujar_ultimo_evento(const EstadoVista *estado) {
    if (estado->ultimo_evento[0] == '\0') {
        return;
    }
    char texto[EVENTO_MAX + NOMBRE_MAX + 16];
    snprintf(texto, sizeof(texto), "Ultimo: %s", estado->ultimo_evento);
    int ancho = MeasureText(texto, RENDER_EVENTO_FONT_SIZE);
    int x = VENTANA_ANCHO - ancho - RENDER_HUD_MARGEN;
    int y = VENTANA_ALTO - RENDER_EVENTO_FONT_SIZE - RENDER_HUD_MARGEN;
    DrawText(texto, x, y, RENDER_EVENTO_FONT_SIZE, LIGHTGRAY);
}

/* Dibuja un alien (o el OVNI) segun tipo. */
static void render_dibujar_alien(const EntidadVista *alien) {
    Color color = render_color_alien(alien->tipo);
    if (alien->tipo == TIPO_VISTA_OVNI) {
        DrawRectangle(alien->x, alien->y, RENDER_ANCHO_OVNI, RENDER_ALTO_OVNI, color);
    } else {
        DrawRectangle(alien->x, alien->y, RENDER_ANCHO_ALIEN, RENDER_ALTO_ALIEN, color);
    }
}

/* Dibuja una bala segun el dueno (alien vs canon). */
static void render_dibujar_bala(const EntidadVista *bala) {
    Color color = (strncmp(bala->etiqueta, RENDER_ETIQUETA_BALA_ALIEN, ID_MAX) == 0)
                      ? RED
                      : YELLOW;
    DrawRectangle(bala->x, bala->y, RENDER_ANCHO_BALA, RENDER_ALTO_BALA, color);
}

/* Dibuja un bunker con alpha segun salud. Si salud == 0 no dibuja nada. */
static void render_dibujar_bunker(const EntidadVista *bunker) {
    if (bunker->extra <= 0) {
        return;
    }
    Color color = GREEN;
    if (bunker->extra >= RENDER_BUNKER_SALUD_ALTA) {
        color.a = 255;
    } else if (bunker->extra >= RENDER_BUNKER_SALUD_MEDIA) {
        color.a = (unsigned char)(255 * 60 / 100);
    } else {
        color.a = (unsigned char)(255 * 30 / 100);
    }
    DrawRectangle(bunker->x, bunker->y, RENDER_ANCHO_BUNKER, RENDER_ALTO_BUNKER, color);
}

/* Dibuja un canon. El canon "destacado" (jugador local o target observado) va
 * en verde brillante; el resto en celeste. */
static void render_dibujar_canon(const EntidadVista *canon, const char *id_destacado) {
    /* El servidor guarda el id del jugador dueno del canon en la etiqueta. */
    bool es_destacado = (id_destacado != NULL &&
                         strncmp(canon->etiqueta, id_destacado, ID_MAX) == 0);
    Color color = es_destacado ? LIME : SKYBLUE;
    DrawRectangle(canon->x, canon->y, RENDER_ANCHO_CANNON, RENDER_ALTO_CANNON, color);
}

/* Overlay de GAME OVER centrado. */
static void render_dibujar_gameover(void) {
    const char *texto = "GAME OVER";
    int ancho = MeasureText(texto, RENDER_GAMEOVER_FONT_SIZE);
    int x = (VENTANA_ANCHO - ancho) / 2;
    int y = (VENTANA_ALTO - RENDER_GAMEOVER_FONT_SIZE) / 2;
    /* fondo semitransparente para resaltar el texto */
    DrawRectangle(0, 0, VENTANA_ANCHO, VENTANA_ALTO, (Color){0, 0, 0, 180});
    DrawText(texto, x, y, RENDER_GAMEOVER_FONT_SIZE, RED);
}

/* ===== API publica ===== */

void render_inicializar(void) {
    SetTraceLogLevel(LOG_WARNING); /* menos ruido en stdout */
    InitWindow(VENTANA_ANCHO, VENTANA_ALTO, VENTANA_TITULO);
    SetTargetFPS(FPS);
}

void render_dibujar(const EstadoVista *estado,
                    const char *id_jugador_local,
                    const char *target_observado,
                    int soy_espectador) {
    /* Id que se "resalta" en el render (canon verde + marcador en sidebar). */
    const char *id_destacado = soy_espectador ? target_observado : id_jugador_local;

    BeginDrawing();
    ClearBackground(BLACK);

    /* Bunkers de fondo (antes que balas y aliens para que las balas se vean por encima). */
    for (int i = 0; i < estado->n_bunkers; i++) {
        render_dibujar_bunker(&estado->bunkers[i]);
    }

    /* Aliens. */
    for (int i = 0; i < estado->n_aliens; i++) {
        render_dibujar_alien(&estado->aliens[i]);
    }

    /* OVNI si esta presente. */
    if (estado->ovni_presente) {
        render_dibujar_alien(&estado->ovni);
    }

    /* Canones. */
    for (int i = 0; i < estado->n_canones; i++) {
        render_dibujar_canon(&estado->canones[i], id_destacado);
    }

    /* Balas. */
    for (int i = 0; i < estado->n_balas; i++) {
        render_dibujar_bala(&estado->balas[i]);
    }

    /* HUD encima de todo. */
    if (soy_espectador) {
        render_dibujar_hud_espectador(estado, target_observado);
    } else {
        render_dibujar_hud(estado, id_jugador_local);
    }
    render_dibujar_sidebar(estado, id_destacado);
    render_dibujar_ultimo_evento(estado);

    if (estado->juego_terminado) {
        render_dibujar_gameover();
    }

    EndDrawing();
}

void render_dibujar_esperando(const char *host, int puerto) {
    BeginDrawing();
    ClearBackground(BLACK);
    char texto[128];
    snprintf(texto, sizeof(texto), "Conectando a %s:%d ...", host, puerto);
    int ancho = MeasureText(texto, RENDER_HUD_FONT_SIZE);
    int x = (VENTANA_ANCHO - ancho) / 2;
    int y = (VENTANA_ALTO - RENDER_HUD_FONT_SIZE) / 2;
    DrawText(texto, x, y, RENDER_HUD_FONT_SIZE, RAYWHITE);
    EndDrawing();
}

void render_dibujar_desconectado(const char *mensaje) {
    BeginDrawing();
    ClearBackground(BLACK);
    int ancho = MeasureText(mensaje, RENDER_HUD_FONT_SIZE);
    int x = (VENTANA_ANCHO - ancho) / 2;
    int y = (VENTANA_ALTO - RENDER_HUD_FONT_SIZE) / 2;
    DrawText(mensaje, x, y, RENDER_HUD_FONT_SIZE, RED);
    EndDrawing();
}

void render_cerrar(void) {
    if (IsWindowReady()) {
        CloseWindow();
    }
}
