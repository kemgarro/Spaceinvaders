#ifndef AUDIO_H
#define AUDIO_H

/*
 * audio.h
 * -------
 * Capa de audio del cliente. Encapsula raylib audio: ningun otro modulo
 * conoce la libreria. Los sonidos se generan en memoria al inicializar
 * (sin assets externos en disco): forma de onda sintetizada con seno,
 * ruido o sweep segun el efecto.
 *
 * El cliente llama a:
 *   - audio_inicializar()     una vez al arranque, despues de raylib.
 *   - audio_<efecto>()        cada vez que un evento del juego dispara
 *                             un sonido (disparo, explosion, etc).
 *   - audio_cerrar()          una vez al final, antes de cerrar raylib.
 */

/** Tasa de muestreo (Hz) y profundidad de bits (16) usados para sintetizar. */
#define AUDIO_SAMPLE_RATE     44100
#define AUDIO_SAMPLE_SIZE_BITS 16
#define AUDIO_CHANNELS         1

/** Duraciones (segundos) de cada efecto. */
#define AUDIO_DUR_DISPARO       0.12f
#define AUDIO_DUR_EXPLOSION     0.30f
#define AUDIO_DUR_OVNI          0.60f
#define AUDIO_DUR_VIDA_PERDIDA  0.45f

/**
 * Inicializa el subsistema de audio y sintetiza los sonidos del juego.
 * Si InitAudioDevice falla (sin audio en el sistema), las llamadas
 * posteriores a audio_* son no-ops silenciosos.
 */
void audio_inicializar(void);

/** Sonido corto tipo laser para disparo del canon. */
void audio_disparo(void);

/** Boom grave + ruido cuando muere un alien o el OVNI. */
void audio_explosion(void);

/** Warble / sirena suave mientras el OVNI cruza la pantalla. */
void audio_ovni(void);

/** Tono grave descendente cuando el jugador pierde una vida. */
void audio_vida_perdida(void);

/** Libera buffers y cierra el dispositivo de audio. */
void audio_cerrar(void);

#endif /* AUDIO_H */
