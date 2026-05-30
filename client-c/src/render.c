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

/* Dibuja el HUD superior con puntaje/vidas/oleada del jugador local. */
static void render_dibujar_hud(const EstadoVista *estado, const char *id_jugador_local) {
    char texto[128];
    const JugadorVista *yo = NULL;

    for (int i = 0; i < estado->n_jugadores; i++) {
        if (strncmp(estado->jugadores[i].id, id_jugador_local, ID_MAX) == 0) {
            yo = &estado->jugadores[i];
            break;
        }
    }

    if (yo != NULL) {
        snprintf(texto, sizeof(texto), "Puntaje: %d  Vidas: %d  Oleada: %d",
                 yo->puntaje, yo->vidas, estado->oleada);
    } else {
        snprintf(texto, sizeof(texto), "esperando estado...");
    }

    DrawText(texto, RENDER_HUD_MARGEN, RENDER_HUD_MARGEN, RENDER_HUD_FONT_SIZE, RAYWHITE);
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

/* Dibuja un canon. El que pertenece al jugador local va en verde brillante. */
static void render_dibujar_canon(const EntidadVista *canon, const char *id_jugador_local) {
    /* El servidor guarda el id del jugador dueno del canon en la etiqueta. */
    bool es_local = (strncmp(canon->etiqueta, id_jugador_local, ID_MAX) == 0);
    Color color = es_local ? LIME : SKYBLUE;
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

void render_dibujar(const EstadoVista *estado, const char *id_jugador_local) {
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
        render_dibujar_canon(&estado->canones[i], id_jugador_local);
    }

    /* Balas. */
    for (int i = 0; i < estado->n_balas; i++) {
        render_dibujar_bala(&estado->balas[i]);
    }

    /* HUD encima de todo. */
    render_dibujar_hud(estado, id_jugador_local);
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
