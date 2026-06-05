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

/* ===== Starfield procedural ===== */
#define RENDER_NUM_ESTRELLAS             120
#define RENDER_ESTRELLA_RADIO_MIN        1
#define RENDER_ESTRELLA_RADIO_MAX        2
#define RENDER_ESTRELLA_ALPHA_BASE       110
#define RENDER_ESTRELLA_ALPHA_PULSO      80
#define RENDER_ESTRELLA_PERIODO_SEG      2.0f

/* ===== Pixel art procedural (sub-bloques de cada entidad) =====
 * Cada alien y el OVNI se dibujan como composiciones de rectangulos
 * pequenios (mini-pixeles). Estas constantes definen el tamanio del
 * mini-pixel y los offsets internos de cada sprite. */
#define RENDER_PIXEL                     4   /* "pixel grande" del sprite */

/* Squid (10 pts): cuerpo cuadrado + 2 antenas. */
#define RENDER_SQUID_CUERPO_ANCHO        16
#define RENDER_SQUID_CUERPO_ALTO         12
#define RENDER_SQUID_ANTENA_ANCHO        4
#define RENDER_SQUID_ANTENA_ALTO         6

/* Crab (20 pts): cuerpo ancho + 2 brazos laterales. */
#define RENDER_CRAB_CUERPO_ANCHO         20
#define RENDER_CRAB_CUERPO_ALTO          12
#define RENDER_CRAB_BRAZO_ANCHO          4
#define RENDER_CRAB_BRAZO_ALTO           6

/* Octopus (40 pts): cuerpo redondeado + 4 tentaculos. */
#define RENDER_OCTOPUS_CUERPO_ANCHO      22
#define RENDER_OCTOPUS_CUERPO_ALTO       12
#define RENDER_OCTOPUS_TENTACULO_ANCHO   3
#define RENDER_OCTOPUS_TENTACULO_ALTO    6

/* OVNI: cuerpo achatado + cupula + 3 luces inferiores. */
#define RENDER_OVNI_CUERPO_ALTO          8
#define RENDER_OVNI_CUPULA_ANCHO         18
#define RENDER_OVNI_CUPULA_ALTO          8
#define RENDER_OVNI_LUZ_RADIO            2
#define RENDER_OVNI_LUZ_SEPARACION       10

/* Canon: base ancha + torre angosta. */
#define RENDER_CANNON_BASE_ALTO          10
#define RENDER_CANNON_TORRE_ANCHO        10
#define RENDER_CANNON_TORRE_ALTO         10

/* Balas: cuerpo + cola de glow detras. */
#define RENDER_BALA_GLOW_ALTO            6
#define RENDER_BALA_GLOW_ALPHA           90

/* Bunker: scoop superior (hueco) + huecos progresivos segun dano. */
#define RENDER_BUNKER_SCOOP_ANCHO        20
#define RENDER_BUNKER_SCOOP_ALTO         12
#define RENDER_BUNKER_HUECO_ANCHO        8
#define RENDER_BUNKER_HUECO_ALTO         8

/* ===== HUD pulido ===== */
#define RENDER_HUD_PUNTAJE_FONT_SIZE     26
#define RENDER_HUD_SEPARADOR_ALTO        2
#define RENDER_HUD_SEPARADOR_Y           38

/* ===== Game Over overlay ===== */
#define RENDER_GAMEOVER_OUTLINE_OFFSET   3

/* ===== Animacion de aliens (frame alternation) ===== */
#define RENDER_FRAME_PERIODO_SEG         0.5f
#define RENDER_FRAME_DESPLAZAMIENTO_ALT  2    /* offset en pixels del frame 1 */

/* ===== Explosiones (efecto al morir alien/ovni) ===== */
#define RENDER_MAX_EXPLOSIONES           64
#define RENDER_EXPLOSION_DURACION_SEG    0.35f
#define RENDER_EXPLOSION_RADIO_INICIAL   6
#define RENDER_EXPLOSION_RADIO_FINAL     22
#define RENDER_EXPLOSION_GROSOR          3

/* ===== Pantalla de inicio (splash) ===== */
#define RENDER_INICIO_TITULO_FONT_SIZE   60
#define RENDER_INICIO_HINT_FONT_SIZE     22
#define RENDER_INICIO_HINT_PARPADEO_SEG  1.0f

/* ===== Animacion del canon al perder vida ===== */
#define RENDER_CANON_HIT_DURACION_SEG    0.55f
#define RENDER_CANON_HIT_SHAKE_AMPLITUD  6     /* pixeles maximo */
#define RENDER_CANON_HIT_SHAKE_FREQ      30.0f /* rad/s */
#define RENDER_MAX_CANON_HITS            4     /* uno por jugador maximo */

/** Inicializa la ventana de raylib. Llamar una vez al arranque. */
void render_inicializar(void);

/**
 * Dibuja la pantalla de inicio (splash). Muestra el titulo del juego y
 * un hint parpadeante "PRESIONA ESPACIO PARA EMPEZAR". El cliente debe
 * llamar esta funcion en bucle hasta que el usuario presione espacio o
 * cierre la ventana.
 *
 * @param soy_espectador 0 = modo jugador, 1 = modo espectador (cambia
 *                       el subtitulo).
 * @param target id del jugador a observar (solo en modo espectador).
 */
void render_dibujar_inicio(int soy_espectador, const char *target);

/**
 * Dispara la animacion de impacto en el canon del jugador indicado.
 * El render aplica shake + tinte rojo al canon durante
 * {@code RENDER_CANON_HIT_DURACION_SEG} segundos. Lo llama main.c
 * cuando el servidor reporta PLAYER_HIT, PLAYER_LIFE_LOST o
 * PLAYER_ELIMINATED.
 *
 * @param jugador_id id del jugador cuyo canon recibio el impacto. Si
 *                   {@code NULL} o vacio, no hace nada.
 */
void render_canon_hit(const char *jugador_id);

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
