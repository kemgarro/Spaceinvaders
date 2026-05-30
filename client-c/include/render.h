#ifndef RENDER_H
#define RENDER_H

#include "protocol.h"

/*
 * render.h
 * --------
 * Capa de presentacion con raylib. Toma un EstadoVista (snapshot recibido del
 * servidor) y lo dibuja en pantalla. Todas las dimensiones de las cajitas y
 * los colores nombrados viven aqui: el resto del cliente no toca raylib.
 *
 * Convenciones:
 *  - El campo del juego es del mismo tamanio que la ventana (VENTANA_ANCHO/ALTO).
 *  - El cannon del jugador local se pinta en color distinto al de los demas.
 *  - Las balas se distinguen por la etiqueta de su dueno (ALIEN vs canon).
 *  - Los bunkers cambian su transparencia segun salud (campo extra).
 */

/* ===== Dimensiones de render (cajitas) ===== */
#define RENDER_ANCHO_ALIEN     28
#define RENDER_ALTO_ALIEN      20
#define RENDER_ANCHO_CANNON    40
#define RENDER_ALTO_CANNON     20
#define RENDER_ANCHO_BALA      3
#define RENDER_ALTO_BALA       10
#define RENDER_ANCHO_BUNKER    60
#define RENDER_ALTO_BUNKER     40
#define RENDER_ANCHO_OVNI      40
#define RENDER_ALTO_OVNI       20

/* ===== Umbrales de salud del bunker para alpha ===== */
#define RENDER_BUNKER_SALUD_ALTA   70
#define RENDER_BUNKER_SALUD_MEDIA  40

/* ===== HUD ===== */
#define RENDER_HUD_FONT_SIZE       20
#define RENDER_GAMEOVER_FONT_SIZE  60
#define RENDER_EVENTO_FONT_SIZE    16
#define RENDER_HUD_MARGEN          10

/* ===== Sidebar de jugadores ===== */
#define RENDER_SIDEBAR_ANCHO       180
#define RENDER_SIDEBAR_MARGEN_DER  20
#define RENDER_SIDEBAR_Y_INICIO    80
#define RENDER_SIDEBAR_FONT_SIZE   16
#define RENDER_SIDEBAR_INTERLINEA  22
#define RENDER_SIDEBAR_TITULO_Y    80

/** Inicializa la ventana de raylib. Llamar una vez al arranque. */
void render_inicializar(void);

/**
 * Dibuja un frame completo a partir del estado actual. Llamar dentro del loop.
 * soy_espectador: 0 = cliente jugador, 1 = cliente espectador.
 * id_jugador_local: id del jugador local (solo se usa cuando soy_espectador == 0).
 * target_observado: id del jugador observado (solo se usa cuando soy_espectador == 1).
 *                   Puede ser NULL en modo jugador.
 */
void render_dibujar(const EstadoVista *estado,
                    const char *id_jugador_local,
                    const char *target_observado,
                    int soy_espectador);

/** Dibuja una pantalla de "conectando..." mientras no hay estado aun. */
void render_dibujar_esperando(const char *host, int puerto);

/** Dibuja una pantalla de "desconectado / error" antes de cerrar. */
void render_dibujar_desconectado(const char *mensaje);

/** Cierra la ventana de raylib. Llamar una vez al final. */
void render_cerrar(void);

#endif /* RENDER_H */
