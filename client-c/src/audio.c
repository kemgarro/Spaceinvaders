/*
 * audio.c
 * -------
 * Implementacion del modulo de audio: sintetiza 4 efectos en memoria al
 * arranque y los reproduce con PlaySound de raylib.
 *
 * Forma de generar los sonidos:
 *   - Wave struct con frameCount, sampleRate, sampleSize=16, channels=1.
 *   - Buffer de samples de tipo int16_t, alocado con malloc.
 *   - Cada efecto llena el buffer con un patron distinto (seno, sweep,
 *     ruido, vibrato) y luego se carga con LoadSoundFromWave.
 *
 * raylib copia internamente los samples al crear el Sound, asi que es
 * seguro liberar el Wave con UnloadWave inmediatamente despues.
 */

#include <math.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>

#include "raylib.h"

#include "audio.h"

/* ===== Estado del modulo ===== */
static int g_audio_disponible = 0;
static Sound g_sonido_disparo;
static Sound g_sonido_explosion;
static Sound g_sonido_ovni;
static Sound g_sonido_vida_perdida;

/* Volumen general aplicado al amplitude antes de cuantizar a int16. */
#define AUDIO_AMPLITUD_MAX  20000   /* 0..32767, dejamos headroom */

/* ===== Helpers privados ===== */

/* Aloca un buffer de N samples int16 y devuelve el puntero. El llamador
 * es responsable de pasarlo a UnloadWave despues de LoadSoundFromWave. */
static int16_t *audio_alocar_buffer(int n_samples) {
    int16_t *buf = (int16_t *)malloc(sizeof(int16_t) * (size_t)n_samples);
    if (buf == NULL) {
        fprintf(stderr, "audio: malloc fallo para %d samples\n", n_samples);
        return NULL;
    }
    return buf;
}

/* Construye una Wave de raylib con los samples dados, la cargo como Sound
 * y libero el Wave (raylib copia internamente). */
static Sound audio_crear_sound(int16_t *samples, int n_samples) {
    Wave wave = (Wave){0};
    wave.frameCount = (unsigned int)n_samples;
    wave.sampleRate = AUDIO_SAMPLE_RATE;
    wave.sampleSize = AUDIO_SAMPLE_SIZE_BITS;
    wave.channels   = AUDIO_CHANNELS;
    wave.data = samples;
    Sound s = LoadSoundFromWave(wave);
    UnloadWave(wave);  /* libera el buffer interno; raylib ya copio. */
    return s;
}

/* Sintetiza un sweep de frecuencia (de f_inicio a f_fin) modulado por una
 * envelope lineal descendente. Ideal para "pew" del disparo. */
static Sound audio_sintetizar_disparo(void) {
    int n = (int)(AUDIO_DUR_DISPARO * AUDIO_SAMPLE_RATE);
    int16_t *buf = audio_alocar_buffer(n);
    if (buf == NULL) return (Sound){0};

    const float f_inicio = 880.0f;
    const float f_fin    = 220.0f;
    float fase = 0.0f;
    for (int i = 0; i < n; i++) {
        float t = (float)i / (float)n;            /* 0..1 */
        float f = f_inicio + (f_fin - f_inicio) * t;
        fase += 2.0f * (float)M_PI * f / (float)AUDIO_SAMPLE_RATE;
        float envelope = 1.0f - t;                /* decae a 0 */
        float s = sinf(fase) * envelope;
        buf[i] = (int16_t)(s * AUDIO_AMPLITUD_MAX);
    }
    return audio_crear_sound(buf, n);
}

/* Mezcla un tono grave + ruido blanco con envelope exponencial.
 * Aproxima la sensacion de "boom". */
static Sound audio_sintetizar_explosion(void) {
    int n = (int)(AUDIO_DUR_EXPLOSION * AUDIO_SAMPLE_RATE);
    int16_t *buf = audio_alocar_buffer(n);
    if (buf == NULL) return (Sound){0};

    const float f_boom = 70.0f;
    float fase = 0.0f;
    for (int i = 0; i < n; i++) {
        float t = (float)i / (float)n;
        fase += 2.0f * (float)M_PI * f_boom / (float)AUDIO_SAMPLE_RATE;
        float boom = sinf(fase);
        /* Ruido blanco simple. */
        float ruido = ((float)rand() / (float)RAND_MAX) * 2.0f - 1.0f;
        float mezcla = boom * 0.6f + ruido * 0.4f;
        /* Envelope exponencial: cae rapido al inicio, lento al final. */
        float envelope = expf(-3.0f * t);
        buf[i] = (int16_t)(mezcla * envelope * AUDIO_AMPLITUD_MAX);
    }
    return audio_crear_sound(buf, n);
}

/* Vibrato: tono base modulado por una sinusoide lenta. Simula el OVNI
 * clasico de Space Invaders. */
static Sound audio_sintetizar_ovni(void) {
    int n = (int)(AUDIO_DUR_OVNI * AUDIO_SAMPLE_RATE);
    int16_t *buf = audio_alocar_buffer(n);
    if (buf == NULL) return (Sound){0};

    const float f_base   = 440.0f;
    const float f_mod    = 8.0f;     /* frecuencia del vibrato */
    const float profundidad = 80.0f; /* +- Hz */
    float fase = 0.0f;
    for (int i = 0; i < n; i++) {
        float t = (float)i / (float)AUDIO_SAMPLE_RATE;
        float f = f_base + profundidad * sinf(2.0f * (float)M_PI * f_mod * t);
        fase += 2.0f * (float)M_PI * f / (float)AUDIO_SAMPLE_RATE;
        /* Envelope con ataque + decay suave (forma de campana). */
        float p = (float)i / (float)n;
        float envelope = sinf((float)M_PI * p);
        float s = sinf(fase) * envelope;
        buf[i] = (int16_t)(s * AUDIO_AMPLITUD_MAX);
    }
    return audio_crear_sound(buf, n);
}

/* Slide descendente de 440Hz a 110Hz: sonido de "fail" / vida perdida. */
static Sound audio_sintetizar_vida_perdida(void) {
    int n = (int)(AUDIO_DUR_VIDA_PERDIDA * AUDIO_SAMPLE_RATE);
    int16_t *buf = audio_alocar_buffer(n);
    if (buf == NULL) return (Sound){0};

    const float f_inicio = 440.0f;
    const float f_fin    = 110.0f;
    float fase = 0.0f;
    for (int i = 0; i < n; i++) {
        float t = (float)i / (float)n;
        float f = f_inicio + (f_fin - f_inicio) * t;
        fase += 2.0f * (float)M_PI * f / (float)AUDIO_SAMPLE_RATE;
        float envelope = 1.0f - 0.6f * t; /* leve decay */
        float s = sinf(fase) * envelope;
        buf[i] = (int16_t)(s * AUDIO_AMPLITUD_MAX);
    }
    return audio_crear_sound(buf, n);
}

/* ===== API publica ===== */

void audio_inicializar(void) {
    InitAudioDevice();
    if (!IsAudioDeviceReady()) {
        fprintf(stderr, "audio: dispositivo no disponible, sonidos deshabilitados\n");
        g_audio_disponible = 0;
        return;
    }
    g_sonido_disparo       = audio_sintetizar_disparo();
    g_sonido_explosion     = audio_sintetizar_explosion();
    g_sonido_ovni          = audio_sintetizar_ovni();
    g_sonido_vida_perdida  = audio_sintetizar_vida_perdida();
    g_audio_disponible = 1;
}

void audio_disparo(void) {
    if (g_audio_disponible) PlaySound(g_sonido_disparo);
}

void audio_explosion(void) {
    if (g_audio_disponible) PlaySound(g_sonido_explosion);
}

void audio_ovni(void) {
    if (g_audio_disponible) PlaySound(g_sonido_ovni);
}

void audio_vida_perdida(void) {
    if (g_audio_disponible) PlaySound(g_sonido_vida_perdida);
}

void audio_cerrar(void) {
    if (!g_audio_disponible) {
        CloseAudioDevice();
        return;
    }
    UnloadSound(g_sonido_disparo);
    UnloadSound(g_sonido_explosion);
    UnloadSound(g_sonido_ovni);
    UnloadSound(g_sonido_vida_perdida);
    CloseAudioDevice();
    g_audio_disponible = 0;
}
