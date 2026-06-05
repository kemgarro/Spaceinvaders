/*
 * render.c
 * --------
 * Implementacion del modulo de render con raylib. Se evita exponer Color o
 * cualquier tipo de raylib hacia los headers para no acoplar el resto del
 * cliente a la libreria grafica.
 *
 * Pulido visual:
 *   - Fondo de estrellas procedural con titileo sinusoidal.
 *   - Aliens dibujados como composiciones de "mini-pixeles" (sprite estilo
 *     pixel art clasico de Space Invaders), distintos por tipo.
 *   - Canon con base + torre, OVNI con cupula + luces, balas con cola glow.
 *   - Bunker con scoop superior y huecos progresivos segun dano.
 *   - HUD con puntaje destacado y separador.
 *   - Game over con outline para legibilidad.
 */

#include <math.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "raylib.h"

#include "constants.h"
#include "protocol.h"
#include "render.h"

/* ===== Starfield: posiciones y fases generadas una sola vez ===== */
typedef struct {
    int x, y;
    int radio;
    float fase;   /* offset en radianes para el titileo */
} Estrella;

static Estrella g_estrellas[RENDER_NUM_ESTRELLAS];
static int g_starfield_inicializado = 0;

/* ===== Explosiones (efecto al morir alien/ovni) =====
 * Cada explosion se dibuja como una cruz amarilla cuyo radio crece y
 * cuyo alpha decae con el tiempo. Las explosiones se disparan SIN
 * cambios al protocolo: al detectar que un alien presente en el frame
 * anterior ya no esta en el frame actual, se asume que murio y se
 * spawnea una explosion en su ultima posicion conocida. */
typedef struct {
    int x, y;
    float tiempo_inicio;
    int activa;
} Explosion;

/* Static array: C garantiza que se inicializa en cero (todas inactivas). */
static Explosion g_explosiones[RENDER_MAX_EXPLOSIONES];

/* Cache del frame anterior para detectar aliens recien muertos. */
static EntidadVista g_aliens_previo[MAX_ALIENS_VISTA];
static int g_n_aliens_previo = 0;
static EntidadVista g_ovni_previo;
static int g_ovni_previo_presente = 0;

/* ===== Animaciones de impacto en el canon =====
 * Cuando un jugador recibe un disparo, registramos un "hit" asociado a su
 * id con la marca de tiempo del evento. Mientras dura, el canon se dibuja
 * con shake horizontal + tinte rojo decayente. */
typedef struct {
    char jugador_id[ID_MAX];
    float tiempo_inicio;
    int activo;
} CanonHit;

static CanonHit g_canon_hits[RENDER_MAX_CANON_HITS];

/* ===== Animacion de aliens (frame counter compartido) =====
 * Todos los aliens del bloque alternan su sprite al mismo tiempo, igual
 * que en el Space Invaders original. La fuente del tiempo es GetTime()
 * para que la animacion no dependa del frame rate del cliente. */
static int render_frame_actual(void) {
    return ((int)(GetTime() / RENDER_FRAME_PERIODO_SEG)) & 1;
}

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

/* Dibuja el HUD superior con puntaje/vidas/oleada del jugador local. El
 * puntaje va destacado en fuente grande; las vidas y la oleada van en
 * una linea secundaria. Un separador horizontal cierra el bloque. */
static void render_dibujar_hud(const EstadoVista *estado, const char *id_jugador_local) {
    const JugadorVista *yo = render_buscar_jugador(estado, id_jugador_local);

    if (yo == NULL) {
        DrawText("esperando estado...",
                 RENDER_HUD_MARGEN, RENDER_HUD_MARGEN,
                 RENDER_HUD_FONT_SIZE, RAYWHITE);
        return;
    }

    /* Linea 1: PUNTAJE destacado en grande. */
    char puntaje[64];
    snprintf(puntaje, sizeof(puntaje), "PUNTAJE  %06d", yo->puntaje);
    DrawText(puntaje, RENDER_HUD_MARGEN, RENDER_HUD_MARGEN,
             RENDER_HUD_PUNTAJE_FONT_SIZE, RAYWHITE);

    /* Linea 2: vidas y oleada en fuente normal. */
    char detalle[64];
    snprintf(detalle, sizeof(detalle), "VIDAS  %d     OLEADA  %d",
             yo->vidas, estado->oleada);
    DrawText(detalle,
             RENDER_HUD_MARGEN,
             RENDER_HUD_MARGEN + RENDER_HUD_PUNTAJE_FONT_SIZE - 2,
             RENDER_HUD_FONT_SIZE, LIGHTGRAY);

    /* Separador horizontal debajo del HUD. */
    DrawRectangle(RENDER_HUD_MARGEN, RENDER_HUD_SEPARADOR_Y + 12,
                  VENTANA_ANCHO - 2 * RENDER_HUD_MARGEN - RENDER_SIDEBAR_ANCHO,
                  RENDER_HUD_SEPARADOR_ALTO,
                  (Color){ 80, 80, 80, 180 });
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

/* Inicializa el starfield con posiciones aleatorias deterministas (srand fijo
 * para que se vea estable entre frames pero distinto entre arranques). */
static void render_inicializar_starfield(void) {
    if (g_starfield_inicializado) {
        return;
    }
    /* Semilla fija para que el patron de estrellas sea reproducible entre
     * ejecuciones (mejor para defensa: la misma "noche" en cada sesion). */
    srand(0xC4F3);
    for (int i = 0; i < RENDER_NUM_ESTRELLAS; i++) {
        g_estrellas[i].x = rand() % VENTANA_ANCHO;
        g_estrellas[i].y = rand() % VENTANA_ALTO;
        int rango = RENDER_ESTRELLA_RADIO_MAX - RENDER_ESTRELLA_RADIO_MIN + 1;
        g_estrellas[i].radio = RENDER_ESTRELLA_RADIO_MIN + (rand() % rango);
        g_estrellas[i].fase = (float)(rand() % 628) / 100.0f; /* 0..2*PI */
    }
    g_starfield_inicializado = 1;
}

/* Busca el slot libre mas viejo (o reutiliza el slot 0 si todos estan
 * activos). Devuelve un indice valido en g_explosiones. */
static int render_explosion_slot_libre(void) {
    for (int i = 0; i < RENDER_MAX_EXPLOSIONES; i++) {
        if (!g_explosiones[i].activa) return i;
    }
    /* Todos ocupados: pisamos el primero (caso raro). */
    return 0;
}

/* Registra una nueva explosion en (x, y). Si no quedan slots libres
 * reusa el slot 0 (las explosiones duran ~0.35s asi que esto es raro). */
static void render_spawn_explosion(int x, int y) {
    int slot = render_explosion_slot_libre();
    g_explosiones[slot].x = x;
    g_explosiones[slot].y = y;
    g_explosiones[slot].tiempo_inicio = (float)GetTime();
    g_explosiones[slot].activa = 1;
}

/* Busca un alien del frame actual con el mismo id que el dado.
 * Devuelve 1 si lo encuentra, 0 si no. */
static int render_alien_en_frame_actual(const EstadoVista *estado, const char *id) {
    for (int i = 0; i < estado->n_aliens; i++) {
        if (strncmp(estado->aliens[i].id, id, ID_MAX) == 0) {
            return 1;
        }
    }
    return 0;
}

/* Compara el frame anterior con el actual. Por cada alien (o el OVNI)
 * que estaba antes y ya no esta, spawnea una explosion. Despues actualiza
 * el cache con el estado actual. */
static void render_detectar_muertes(const EstadoVista *estado) {
    /* Aliens. */
    for (int i = 0; i < g_n_aliens_previo; i++) {
        const EntidadVista *prev = &g_aliens_previo[i];
        if (!render_alien_en_frame_actual(estado, prev->id)) {
            /* Centrar la explosion en el centro del sprite. */
            render_spawn_explosion(prev->x + RENDER_ANCHO_ALIEN / 2,
                                   prev->y + RENDER_ALTO_ALIEN / 2);
        }
    }
    /* OVNI: si estaba y ya no esta, tambien explota. */
    if (g_ovni_previo_presente && !estado->ovni_presente) {
        render_spawn_explosion(g_ovni_previo.x + RENDER_ANCHO_OVNI / 2,
                               g_ovni_previo.y + RENDER_ALTO_OVNI / 2);
    }

    /* Actualizar el cache. */
    g_n_aliens_previo = estado->n_aliens;
    if (g_n_aliens_previo > MAX_ALIENS_VISTA) {
        g_n_aliens_previo = MAX_ALIENS_VISTA;
    }
    for (int i = 0; i < g_n_aliens_previo; i++) {
        g_aliens_previo[i] = estado->aliens[i];
    }
    g_ovni_previo_presente = estado->ovni_presente;
    if (estado->ovni_presente) {
        g_ovni_previo = estado->ovni;
    }
}

/* Dibuja cada explosion activa como una cruz amarilla cuyo radio crece y
 * cuyo alpha decae. Cuando pasa la duracion total se desactiva. */
static void render_dibujar_explosiones(void) {
    float t = (float)GetTime();
    for (int i = 0; i < RENDER_MAX_EXPLOSIONES; i++) {
        if (!g_explosiones[i].activa) continue;
        float edad = t - g_explosiones[i].tiempo_inicio;
        if (edad >= RENDER_EXPLOSION_DURACION_SEG) {
            g_explosiones[i].activa = 0;
            continue;
        }
        float progreso = edad / RENDER_EXPLOSION_DURACION_SEG; /* 0..1 */
        int radio = RENDER_EXPLOSION_RADIO_INICIAL
                  + (int)((RENDER_EXPLOSION_RADIO_FINAL - RENDER_EXPLOSION_RADIO_INICIAL)
                          * progreso);
        unsigned char alpha = (unsigned char)(255.0f * (1.0f - progreso));
        Color color = (Color){ 255, 220, 80, alpha };
        int cx = g_explosiones[i].x;
        int cy = g_explosiones[i].y;
        /* Cruz: barra horizontal y vertical centradas. */
        DrawRectangle(cx - radio, cy - RENDER_EXPLOSION_GROSOR / 2,
                      radio * 2, RENDER_EXPLOSION_GROSOR, color);
        DrawRectangle(cx - RENDER_EXPLOSION_GROSOR / 2, cy - radio,
                      RENDER_EXPLOSION_GROSOR, radio * 2, color);
    }
}

/* Dibuja las estrellas con titileo sinusoidal usando el tiempo de raylib. */
static void render_dibujar_starfield(void) {
    float t = (float)GetTime();
    float omega = (float)(2.0 * 3.14159265 / RENDER_ESTRELLA_PERIODO_SEG);
    for (int i = 0; i < RENDER_NUM_ESTRELLAS; i++) {
        const Estrella *e = &g_estrellas[i];
        float pulso = sinf(omega * t + e->fase);  /* -1..1 */
        int alpha = RENDER_ESTRELLA_ALPHA_BASE
                  + (int)(RENDER_ESTRELLA_ALPHA_PULSO * pulso);
        if (alpha < 0) alpha = 0;
        if (alpha > 255) alpha = 255;
        Color c = (Color){ 255, 255, 255, (unsigned char)alpha };
        DrawCircle(e->x, e->y, (float)e->radio, c);
    }
}

/* Dibuja un mini-pixel rectangular del sprite a partir de una grilla virtual.
 * (ox, oy) es la esquina superior izquierda del sprite; (fx, fy) las
 * coordenadas en mini-pixeles dentro del sprite. */
static void render_pixel(int ox, int oy, int fx, int fy, int w, int h, Color color) {
    DrawRectangle(ox + fx, oy + fy, w, h, color);
}

/* Squid (10 pts): cuerpo cuadrado pequenio con 2 antenas hacia arriba.
 * Frame 0: antenas pegadas al cuerpo. Frame 1: antenas separadas hacia
 * los costados, como si las moviera al caminar. */
static void render_dibujar_squid(int x, int y, Color color) {
    int frame = render_frame_actual();
    int desp = (frame == 1) ? RENDER_FRAME_DESPLAZAMIENTO_ALT : 0;

    /* Cuerpo centrado. */
    int cx = x + (RENDER_ANCHO_ALIEN - RENDER_SQUID_CUERPO_ANCHO) / 2;
    int cy = y + RENDER_SQUID_ANTENA_ALTO;
    render_pixel(cx, cy, 0, 0,
                 RENDER_SQUID_CUERPO_ANCHO, RENDER_SQUID_CUERPO_ALTO, color);
    /* Antenas separandose / juntandose segun el frame. */
    int ax_izq = cx + RENDER_PIXEL - desp;
    int ax_der = cx + RENDER_SQUID_CUERPO_ANCHO - RENDER_PIXEL - RENDER_SQUID_ANTENA_ANCHO + desp;
    render_pixel(ax_izq, y, 0, 0,
                 RENDER_SQUID_ANTENA_ANCHO, RENDER_SQUID_ANTENA_ALTO, color);
    render_pixel(ax_der, y, 0, 0,
                 RENDER_SQUID_ANTENA_ANCHO, RENDER_SQUID_ANTENA_ALTO, color);
    /* "Ojos" como huecos. */
    int oy_ojos = cy + RENDER_PIXEL;
    Color hueco = BLACK;
    render_pixel(cx + RENDER_PIXEL, oy_ojos, 0, 0, RENDER_PIXEL, RENDER_PIXEL, hueco);
    render_pixel(cx + RENDER_SQUID_CUERPO_ANCHO - 2 * RENDER_PIXEL, oy_ojos, 0, 0,
                 RENDER_PIXEL, RENDER_PIXEL, hueco);
}

/* Crab (20 pts): cuerpo ancho con dos brazos a los lados.
 * Frame 0: brazos al medio del cuerpo. Frame 1: brazos hacia arriba. */
static void render_dibujar_crab(int x, int y, Color color) {
    int frame = render_frame_actual();
    int desp_y = (frame == 1) ? -RENDER_FRAME_DESPLAZAMIENTO_ALT : 0;

    int cy = y + (RENDER_ALTO_ALIEN - RENDER_CRAB_CUERPO_ALTO) / 2;
    int cx = x + (RENDER_ANCHO_ALIEN - RENDER_CRAB_CUERPO_ANCHO) / 2;
    render_pixel(cx, cy, 0, 0,
                 RENDER_CRAB_CUERPO_ANCHO, RENDER_CRAB_CUERPO_ALTO, color);
    /* Brazos izq/der que sobresalen, suben y bajan con el frame. */
    int by = cy + (RENDER_CRAB_CUERPO_ALTO - RENDER_CRAB_BRAZO_ALTO) / 2 + desp_y;
    render_pixel(cx - RENDER_CRAB_BRAZO_ANCHO, by, 0, 0,
                 RENDER_CRAB_BRAZO_ANCHO, RENDER_CRAB_BRAZO_ALTO, color);
    render_pixel(cx + RENDER_CRAB_CUERPO_ANCHO, by, 0, 0,
                 RENDER_CRAB_BRAZO_ANCHO, RENDER_CRAB_BRAZO_ALTO, color);
    /* Ojos. */
    int oy_ojos = cy + RENDER_PIXEL;
    Color hueco = BLACK;
    render_pixel(cx + RENDER_PIXEL, oy_ojos, 0, 0, RENDER_PIXEL, RENDER_PIXEL, hueco);
    render_pixel(cx + RENDER_CRAB_CUERPO_ANCHO - 2 * RENDER_PIXEL, oy_ojos, 0, 0,
                 RENDER_PIXEL, RENDER_PIXEL, hueco);
}

/* Octopus (40 pts): cuerpo el mas ancho con 4 tentaculos hacia abajo.
 * Frame 0: tentaculos pares mas largos (1, 3). Frame 1: tentaculos
 * impares mas largos (2, 4). Simula que ondea los tentaculos al moverse. */
static void render_dibujar_octopus(int x, int y, Color color) {
    int frame = render_frame_actual();

    int cx = x + (RENDER_ANCHO_ALIEN - RENDER_OCTOPUS_CUERPO_ANCHO) / 2;
    int cy = y;
    render_pixel(cx, cy, 0, 0,
                 RENDER_OCTOPUS_CUERPO_ANCHO, RENDER_OCTOPUS_CUERPO_ALTO, color);
    /* 4 tentaculos espaciados, con altura alternada segun el frame. */
    int paso = (RENDER_OCTOPUS_CUERPO_ANCHO - RENDER_OCTOPUS_TENTACULO_ANCHO) / 3;
    int ty = cy + RENDER_OCTOPUS_CUERPO_ALTO;
    for (int i = 0; i < 4; i++) {
        int tx = cx + i * paso;
        /* La paridad del tentaculo decide si esta "extendido". */
        int extendido = ((i % 2) == frame);
        int alto = RENDER_OCTOPUS_TENTACULO_ALTO
                 + (extendido ? RENDER_FRAME_DESPLAZAMIENTO_ALT : 0);
        render_pixel(tx, ty, 0, 0,
                     RENDER_OCTOPUS_TENTACULO_ANCHO, alto, color);
    }
    /* Ojos centrales. */
    int oy_ojos = cy + RENDER_PIXEL;
    Color hueco = BLACK;
    render_pixel(cx + 2 * RENDER_PIXEL, oy_ojos, 0, 0, RENDER_PIXEL, RENDER_PIXEL, hueco);
    render_pixel(cx + RENDER_OCTOPUS_CUERPO_ANCHO - 3 * RENDER_PIXEL, oy_ojos, 0, 0,
                 RENDER_PIXEL, RENDER_PIXEL, hueco);
}

/* OVNI con forma de platillo: cuerpo achatado + cupula encima + 3 luces. */
static void render_dibujar_ovni(int x, int y, Color color) {
    /* Cuerpo del platillo (elipse achatada aproximada con rectangulo). */
    int cuerpo_y = y + RENDER_OVNI_CUPULA_ALTO;
    DrawRectangle(x, cuerpo_y, RENDER_ANCHO_OVNI, RENDER_OVNI_CUERPO_ALTO, color);
    /* Cupula centrada arriba (semi-elipse aproximada con rectangulo
     * + dos bordes en chaflan suave). */
    int cupula_x = x + (RENDER_ANCHO_OVNI - RENDER_OVNI_CUPULA_ANCHO) / 2;
    DrawRectangle(cupula_x, y, RENDER_OVNI_CUPULA_ANCHO, RENDER_OVNI_CUPULA_ALTO,
                  (Color){ color.r, color.g, color.b, 200 });
    /* 3 luces inferiores amarillas. */
    int luz_y = cuerpo_y + RENDER_OVNI_CUERPO_ALTO + RENDER_OVNI_LUZ_RADIO;
    int centro_x = x + RENDER_ANCHO_OVNI / 2;
    DrawCircle(centro_x - RENDER_OVNI_LUZ_SEPARACION, luz_y, RENDER_OVNI_LUZ_RADIO, YELLOW);
    DrawCircle(centro_x, luz_y, RENDER_OVNI_LUZ_RADIO, YELLOW);
    DrawCircle(centro_x + RENDER_OVNI_LUZ_SEPARACION, luz_y, RENDER_OVNI_LUZ_RADIO, YELLOW);
}

/* Dispatch principal: dibuja un alien (o el OVNI) segun tipo. */
static void render_dibujar_alien(const EntidadVista *alien) {
    Color color = render_color_alien(alien->tipo);
    switch (alien->tipo) {
        case TIPO_VISTA_SQUID:   render_dibujar_squid(alien->x, alien->y, color); break;
        case TIPO_VISTA_CRAB:    render_dibujar_crab(alien->x, alien->y, color); break;
        case TIPO_VISTA_OCTOPUS: render_dibujar_octopus(alien->x, alien->y, color); break;
        case TIPO_VISTA_OVNI:    render_dibujar_ovni(alien->x, alien->y, color); break;
        default:
            /* Fallback a rectangulo plano si llega un tipo desconocido. */
            DrawRectangle(alien->x, alien->y,
                          RENDER_ANCHO_ALIEN, RENDER_ALTO_ALIEN, color);
            break;
    }
}

/* Dibuja una bala con cola de glow detras. La direccion del glow depende
 * del dueno: balas de aliens caen (glow arriba), balas del canon suben
 * (glow abajo). */
static void render_dibujar_bala(const EntidadVista *bala) {
    int es_alien = (strncmp(bala->etiqueta, PROTOCOLO_ETIQUETA_BALA_ALIEN, ID_MAX) == 0);
    Color color = es_alien ? RED : YELLOW;
    Color glow  = (Color){ color.r, color.g, color.b, RENDER_BALA_GLOW_ALPHA };

    /* Cuerpo principal de la bala. */
    DrawRectangle(bala->x, bala->y, RENDER_ANCHO_BALA, RENDER_ALTO_BALA, color);

    /* Glow trail: si baja (alien) va sobre la cabeza; si sube (canon) va
     * debajo. La cola es mas tenue y un poco mas ancha. */
    int glow_y = es_alien
                   ? bala->y - RENDER_BALA_GLOW_ALTO
                   : bala->y + RENDER_ALTO_BALA;
    DrawRectangle(bala->x - 1, glow_y,
                  RENDER_ANCHO_BALA + 2, RENDER_BALA_GLOW_ALTO, glow);
}

/* Dibuja un bunker con la silueta clasica de Space Invaders: bloque
 * verde con un "scoop" rectangular arriba (la "boquilla" por donde el
 * canon dispara desde adentro) y huecos progresivos a medida que recibe
 * dano.
 *
 *   - Salud >= 70%: bunker solido (solo el scoop).
 *   - Salud 40..69%: 2 huecos superiores adicionales.
 *   - Salud < 40%:   3 huecos repartidos en toda la cara.
 *   - Salud 0%:      no se dibuja (queda destruido).
 */
static void render_dibujar_bunker(const EntidadVista *bunker) {
    if (bunker->extra <= 0) {
        return;
    }
    /* Color con alpha segun salud (conserva la escala que ya teniamos). */
    Color color = GREEN;
    if (bunker->extra >= RENDER_BUNKER_SALUD_ALTA) {
        color.a = 255;
    } else if (bunker->extra >= RENDER_BUNKER_SALUD_MEDIA) {
        color.a = (unsigned char)(255 * 60 / 100);
    } else {
        color.a = (unsigned char)(255 * 30 / 100);
    }

    /* Cuerpo principal. */
    DrawRectangle(bunker->x, bunker->y, RENDER_ANCHO_BUNKER, RENDER_ALTO_BUNKER, color);

    /* Scoop superior: rectangulo negro centrado que recorta el bunker. */
    int scoop_x = bunker->x + (RENDER_ANCHO_BUNKER - RENDER_BUNKER_SCOOP_ANCHO) / 2;
    DrawRectangle(scoop_x, bunker->y,
                  RENDER_BUNKER_SCOOP_ANCHO, RENDER_BUNKER_SCOOP_ALTO, BLACK);

    /* Huecos de dano. */
    if (bunker->extra < RENDER_BUNKER_SALUD_ALTA) {
        DrawRectangle(bunker->x + RENDER_PIXEL,
                      bunker->y + RENDER_BUNKER_SCOOP_ALTO + RENDER_PIXEL,
                      RENDER_BUNKER_HUECO_ANCHO, RENDER_BUNKER_HUECO_ALTO, BLACK);
        DrawRectangle(bunker->x + RENDER_ANCHO_BUNKER - RENDER_PIXEL - RENDER_BUNKER_HUECO_ANCHO,
                      bunker->y + RENDER_BUNKER_SCOOP_ALTO + RENDER_PIXEL,
                      RENDER_BUNKER_HUECO_ANCHO, RENDER_BUNKER_HUECO_ALTO, BLACK);
    }
    if (bunker->extra < RENDER_BUNKER_SALUD_MEDIA) {
        DrawRectangle(bunker->x + (RENDER_ANCHO_BUNKER - RENDER_BUNKER_HUECO_ANCHO) / 2,
                      bunker->y + RENDER_ALTO_BUNKER - RENDER_BUNKER_HUECO_ALTO - RENDER_PIXEL,
                      RENDER_BUNKER_HUECO_ANCHO, RENDER_BUNKER_HUECO_ALTO, BLACK);
    }
}

/* Busca el slot del hit activo para el id del canon dado. Devuelve un
 * puntero al CanonHit (con progreso 0..1 en *progreso_out) o NULL si el
 * canon no tiene impacto activo en este frame. */
static const CanonHit *render_canon_hit_activo(const char *jugador_id,
                                                float *progreso_out) {
    if (jugador_id == NULL || jugador_id[0] == '\0') return NULL;
    float t = (float)GetTime();
    for (int i = 0; i < RENDER_MAX_CANON_HITS; i++) {
        if (!g_canon_hits[i].activo) continue;
        if (strncmp(g_canon_hits[i].jugador_id, jugador_id, ID_MAX) != 0) continue;
        float edad = t - g_canon_hits[i].tiempo_inicio;
        if (edad >= RENDER_CANON_HIT_DURACION_SEG) {
            g_canon_hits[i].activo = 0;
            continue;
        }
        *progreso_out = edad / RENDER_CANON_HIT_DURACION_SEG;
        return &g_canon_hits[i];
    }
    return NULL;
}

/* Mezcla dos colores con un factor f (0=a, 1=b). Lineal por canal. */
static Color render_color_mezclar(Color a, Color b, float f) {
    if (f < 0.0f) f = 0.0f;
    if (f > 1.0f) f = 1.0f;
    return (Color){
        (unsigned char)(a.r + (b.r - a.r) * f),
        (unsigned char)(a.g + (b.g - a.g) * f),
        (unsigned char)(a.b + (b.b - a.b) * f),
        (unsigned char)(a.a + (b.a - a.a) * f),
    };
}

/* Dibuja un canon con la silueta clasica: base rectangular ancha + torre
 * angosta encima. El canon "destacado" (jugador local o target observado)
 * va en verde brillante; el resto en celeste.
 *
 * Si el canon recibio un impacto recientemente (PLAYER_HIT etc.), se
 * aplica shake horizontal y tinte rojo decayente para dar feedback
 * visual al jugador. */
static void render_dibujar_canon(const EntidadVista *canon, const char *id_destacado) {
    /* El servidor guarda el id del jugador dueno del canon en la etiqueta. */
    bool es_destacado = (id_destacado != NULL &&
                         strncmp(canon->etiqueta, id_destacado, ID_MAX) == 0);
    Color color = es_destacado ? LIME : SKYBLUE;

    /* Aplica shake + tinte si hay impacto activo. */
    int dx = 0;
    float progreso = 0.0f;
    const CanonHit *hit = render_canon_hit_activo(canon->etiqueta, &progreso);
    if (hit != NULL) {
        float restante = 1.0f - progreso;
        /* Shake horizontal: sinusoide de alta frecuencia con amplitud
         * que decae linealmente. */
        float omega = RENDER_CANON_HIT_SHAKE_FREQ;
        float t = (float)GetTime();
        dx = (int)(RENDER_CANON_HIT_SHAKE_AMPLITUD * restante
                   * sinf(omega * t));
        /* Tinte rojo: mezcla el color original con RED, mas intenso al
         * inicio (restante=1) y desvaneciendose. */
        color = render_color_mezclar(color, RED, restante);
    }

    /* Base ancha en la parte baja (con offset de shake). */
    int base_y = canon->y + RENDER_ALTO_CANNON - RENDER_CANNON_BASE_ALTO;
    DrawRectangle(canon->x + dx, base_y,
                  RENDER_ANCHO_CANNON, RENDER_CANNON_BASE_ALTO, color);

    /* Torre centrada arriba de la base, mas angosta. */
    int torre_x = canon->x + (RENDER_ANCHO_CANNON - RENDER_CANNON_TORRE_ANCHO) / 2;
    DrawRectangle(torre_x + dx, canon->y,
                  RENDER_CANNON_TORRE_ANCHO, RENDER_CANNON_TORRE_ALTO, color);
}

/* Overlay de GAME OVER centrado con outline negro y hint para reiniciar. */
static void render_dibujar_gameover(void) {
    const char *texto = "GAME OVER";
    int ancho = MeasureText(texto, RENDER_GAMEOVER_FONT_SIZE);
    int x = (VENTANA_ANCHO - ancho) / 2;
    int y = (VENTANA_ALTO - RENDER_GAMEOVER_FONT_SIZE) / 2;

    /* Fondo semitransparente para resaltar el texto. */
    DrawRectangle(0, 0, VENTANA_ANCHO, VENTANA_ALTO, (Color){0, 0, 0, 180});

    /* Outline: dibujamos el texto en negro desplazado en 4 diagonales y
     * luego el rojo encima. Es un efecto barato pero efectivo para que
     * el "GAME OVER" se lea bien sobre cualquier fondo. */
    int off = RENDER_GAMEOVER_OUTLINE_OFFSET;
    DrawText(texto, x - off, y - off, RENDER_GAMEOVER_FONT_SIZE, BLACK);
    DrawText(texto, x + off, y - off, RENDER_GAMEOVER_FONT_SIZE, BLACK);
    DrawText(texto, x - off, y + off, RENDER_GAMEOVER_FONT_SIZE, BLACK);
    DrawText(texto, x + off, y + off, RENDER_GAMEOVER_FONT_SIZE, BLACK);
    DrawText(texto, x, y, RENDER_GAMEOVER_FONT_SIZE, RED);

    /* Hint "PRESIONA R PARA REINICIAR" debajo del titulo. El cliente
     * envia un INPUT con accion RESTART al servidor cuando el jugador
     * lo presiona; el motor recrea la oleada y reinicia vidas/puntaje. */
    const char *hint = "PRESIONA R PARA REINICIAR";
    int hint_font = 22;
    int hint_ancho = MeasureText(hint, hint_font);
    int hint_x = (VENTANA_ANCHO - hint_ancho) / 2;
    int hint_y = y + RENDER_GAMEOVER_FONT_SIZE + 24;
    DrawText(hint, hint_x, hint_y, hint_font, YELLOW);
}

/* ===== API publica ===== */

void render_inicializar(void) {
    SetTraceLogLevel(LOG_WARNING); /* menos ruido en stdout */
    InitWindow(VENTANA_ANCHO, VENTANA_ALTO, VENTANA_TITULO);
    SetTargetFPS(FPS);
    render_inicializar_starfield();
}

void render_dibujar(const EstadoVista *estado,
                    const char *id_jugador_local,
                    const char *target_observado,
                    int soy_espectador) {
    /* Id que se "resalta" en el render (canon verde + marcador en sidebar). */
    const char *id_destacado = soy_espectador ? target_observado : id_jugador_local;

    /* Detectar muertes (diff con el cache) ANTES de dibujar para que las
     * explosiones del frame actual ya esten activas al renderizar. */
    render_detectar_muertes(estado);

    BeginDrawing();
    ClearBackground(BLACK);

    /* Fondo estrellado (antes de cualquier entidad). */
    render_dibujar_starfield();

    /* Bunkers (antes que balas y aliens para que las balas se vean por encima). */
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

    /* Explosiones (sobre todas las entidades, justo abajo del HUD). */
    render_dibujar_explosiones();

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

void render_canon_hit(const char *jugador_id) {
    if (jugador_id == NULL || jugador_id[0] == '\0') return;

    /* Busca un slot existente para este jugador para "reiniciar" la
     * animacion si recibe varios impactos seguidos; si no, usa el primer
     * slot libre. */
    int slot_existente = -1;
    int slot_libre = -1;
    for (int i = 0; i < RENDER_MAX_CANON_HITS; i++) {
        if (g_canon_hits[i].activo &&
            strncmp(g_canon_hits[i].jugador_id, jugador_id, ID_MAX) == 0) {
            slot_existente = i;
            break;
        }
        if (slot_libre < 0 && !g_canon_hits[i].activo) {
            slot_libre = i;
        }
    }
    int slot = (slot_existente >= 0) ? slot_existente
                                      : (slot_libre >= 0) ? slot_libre : 0;
    snprintf(g_canon_hits[slot].jugador_id, ID_MAX, "%s", jugador_id);
    g_canon_hits[slot].tiempo_inicio = (float)GetTime();
    g_canon_hits[slot].activo = 1;
}

void render_dibujar_inicio(int soy_espectador, const char *target) {
    BeginDrawing();
    ClearBackground(BLACK);

    /* Fondo estrellado tambien en la pantalla de inicio. */
    render_dibujar_starfield();

    /* Titulo grande centrado. */
    const char *titulo = "spaCEinvaders";
    int titulo_ancho = MeasureText(titulo, RENDER_INICIO_TITULO_FONT_SIZE);
    int titulo_x = (VENTANA_ANCHO - titulo_ancho) / 2;
    int titulo_y = VENTANA_ALTO / 3;
    DrawText(titulo, titulo_x, titulo_y, RENDER_INICIO_TITULO_FONT_SIZE, RAYWHITE);

    /* Subtitulo con el modo seleccionado. */
    char subtitulo[64];
    if (soy_espectador) {
        snprintf(subtitulo, sizeof(subtitulo),
                 "Modo espectador (observa %s)",
                 (target != NULL && target[0] != '\0') ? target : "?");
    } else {
        snprintf(subtitulo, sizeof(subtitulo), "Modo jugador");
    }
    int sub_ancho = MeasureText(subtitulo, RENDER_HUD_FONT_SIZE);
    int sub_x = (VENTANA_ANCHO - sub_ancho) / 2;
    int sub_y = titulo_y + RENDER_INICIO_TITULO_FONT_SIZE + 12;
    DrawText(subtitulo, sub_x, sub_y, RENDER_HUD_FONT_SIZE, LIGHTGRAY);

    /* Hint parpadeante "PRESIONA ESPACIO PARA EMPEZAR". El parpadeo es
     * un on/off cada RENDER_INICIO_HINT_PARPADEO_SEG segundos. */
    int visible = ((int)(GetTime() / RENDER_INICIO_HINT_PARPADEO_SEG)) & 1;
    if (visible) {
        const char *hint = "PRESIONA ESPACIO PARA EMPEZAR";
        int hint_ancho = MeasureText(hint, RENDER_INICIO_HINT_FONT_SIZE);
        int hint_x = (VENTANA_ANCHO - hint_ancho) / 2;
        int hint_y = VENTANA_ALTO * 2 / 3;
        DrawText(hint, hint_x, hint_y, RENDER_INICIO_HINT_FONT_SIZE, YELLOW);
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
