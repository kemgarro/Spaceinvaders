/*
 * main.c
 * ------
 * Entry point del cliente C (jugador o espectador).
 *
 * Responsabilidades:
 *  - Parseo simple de argumentos (--host, --port, --id, --spectator, --watch).
 *  - Conexion TCP al servidor via la capa network.
 *  - Handshake CONNECT (incluye target cuando es espectador).
 *  - Game loop: drena STATE/EVENT del servidor, lee input local, envia INPUT,
 *    invoca render. Se mantiene activo hasta que el usuario pide salir, se
 *    pierde la conexion, o llega una senial SIGINT/SIGTERM.
 *  - Despedida DISCONNECT y cierre limpio de la ventana.
 */

#include <signal.h>
#include <stdbool.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "audio.h"
#include "constants.h"
#include "input.h"
#include "network.h"
#include "pico.h"
#include "protocol.h"
#include "render.h"

/* Bandera global para senializar SIGINT/SIGTERM al loop principal. */
static volatile sig_atomic_t solicitud_salir = 0;

/* Handler de senial: solo marca la bandera (operacion async-signal-safe). */
static void manejador_senales(int sig) {
    (void)sig;
    solicitud_salir = 1;
}

/* Imprime ayuda en stderr cuando los argumentos no son validos. */
static void main_imprimir_uso(const char *prog) {
    fprintf(stderr,
            "uso: %s [--host IP] [--port N] [--id ID] [--spectator] [--watch ID] [--pico [device]]\n",
            prog);
}

int main(int argc, char *argv[]) {
    /* --- parseo de argumentos --- */
    const char *host = SERVIDOR_IP_DEFAULT;
    int puerto = SERVIDOR_PUERTO;
    const char *id = CLIENTE_ID_DEFAULT;
    bool spectator = false;
    const char *target_watch = NULL;
    /* NULL = sin Pico. No NULL = ruta del dispositivo serie. */
    const char *pico_device = NULL;

    for (int i = 1; i < argc; i++) {
        if (strcmp(argv[i], "--host") == 0 && i + 1 < argc) {
            host = argv[++i];
        } else if (strcmp(argv[i], "--port") == 0 && i + 1 < argc) {
            puerto = atoi(argv[++i]);
        } else if (strcmp(argv[i], "--id") == 0 && i + 1 < argc) {
            id = argv[++i];
        } else if (strcmp(argv[i], "--spectator") == 0) {
            spectator = true;
        } else if (strcmp(argv[i], "--watch") == 0 && i + 1 < argc) {
            target_watch = argv[++i];
        } else if (strcmp(argv[i], "--pico") == 0) {
            /* Argumento opcional: si lo que sigue no empieza con '-', se toma
             * como ruta del dispositivo; si no, se usa el default. */
            if (i + 1 < argc && argv[i + 1][0] != '-') {
                pico_device = argv[++i];
            } else {
                pico_device = UART_DISPOSITIVO_DEFAULT;
            }
        } else if (strcmp(argv[i], "--help") == 0 || strcmp(argv[i], "-h") == 0) {
            main_imprimir_uso(argv[0]);
            return 0;
        } else {
            fprintf(stderr, "argumento no reconocido: %s\n", argv[i]);
            main_imprimir_uso(argv[0]);
            return 1;
        }
    }

    /* --- validacion de flags --- */
    /* El espectador SIEMPRE necesita target; si no se pasa, salimos antes de
     * abrir el socket para evitar gastar slots en el servidor. */
    if (spectator && (target_watch == NULL || target_watch[0] == '\0')) {
        fprintf(stderr, "modo espectador requiere --watch <id>\n");
        return 2;
    }
    /* En modo jugador --watch no tiene sentido: lo ignoramos pero avisamos. */
    if (!spectator && target_watch != NULL) {
        fprintf(stderr, "aviso: --watch se ignora en modo jugador\n");
        target_watch = NULL;
    }
    /* En modo espectador --pico no tiene sentido: un espectador no manda input. */
    if (spectator && pico_device != NULL) {
        fprintf(stderr, "aviso: --pico se ignora en modo espectador\n");
        pico_device = NULL;
    }

    /* --- instalacion del handler de seniales --- */
    /* Lo dejamos antes de abrir el socket para que cualquier Ctrl+C durante
     * el handshake termine ordenado y no como SIGINT default. */
    struct sigaction sa;
    memset(&sa, 0, sizeof(sa));
    sa.sa_handler = manejador_senales;
    sigemptyset(&sa.sa_mask);
    sigaction(SIGINT, &sa, NULL);
    sigaction(SIGTERM, &sa, NULL);

    /* --- pantalla de inicio (splash) ---
     * Antes de abrir el socket mostramos un splash con "PRESIONA ESPACIO".
     * Asi el server no recibe la conexion hasta que el usuario decide
     * empezar (mas natural que la ventana del juego apareciendo "viva"
     * apenas se lanza el binario). */
    render_inicializar();
    audio_inicializar();
    bool empezar = false;
    while (!input_quiere_salir() && !solicitud_salir && !empezar) {
        render_dibujar_inicio(spectator ? 1 : 0, target_watch);
        if (input_empezar_solicitado()) {
            empezar = true;
        }
    }
    if (!empezar) {
        /* Usuario cerro la ventana antes de empezar: salida limpia. */
        audio_cerrar();
        render_cerrar();
        return 0;
    }

    /* --- conexion al servidor --- */
    Conexion con;
    red_inicializar(&con);
    if (!red_conectar(&con, host, puerto)) {
        fprintf(stderr, "no se pudo conectar a %s:%d\n", host, puerto);
        return 1;
    }

    const char *tipo_cliente = spectator ? TIPO_SPECTATOR : TIPO_PLAYER;
    printf("cliente conectado como %s (%s)\n", id, tipo_cliente);
    if (spectator) {
        printf("observando a %s\n", target_watch);
    }

    char buf[CLIENTE_BUFFER_MENSAJE];
    int n = protocolo_construir_connect(buf, sizeof(buf), id, tipo_cliente, target_watch);
    if (n <= 0 || !red_enviar(&con, buf, n)) {
        fprintf(stderr, "fallo al enviar CONNECT\n");
        red_cerrar(&con);
        return 1;
    }

    /* --- inicializa estado (render ya esta inicializado desde el splash) --- */
    EstadoVista estado;
    protocolo_estado_inicializar(&estado);

    /* --- inicializa Pico (opcional, solo si jugador y se paso --pico) --- */
    ConexionPico pico;
    pico_inicializar(&pico);
    if (pico_device != NULL && !spectator) {
        if (pico_abrir(&pico, pico_device)) {
            printf("pico: conectado a %s (%d baud)\n",
                   pico_device, UART_BAUDRATE_DOC);
        } else {
            printf("pico: no se pudo abrir %s, sigo solo con teclado\n",
                   pico_device);
        }
    }

    bool primer_estado = false;
    char linea[BUFFER_RED];

    /* Cache para disparar audio cuando cambia el estado del servidor. */
    char ultimo_evento_audio[EVENTO_MAX] = "";
    bool ovni_presente_audio = false;

    /* --- loop principal --- */
    while (!input_quiere_salir() && con.conectado && !solicitud_salir) {
        /* drenamos todas las lineas pendientes del servidor */
        while (red_recibir_linea(&con, linea, sizeof(linea))) {
            if (protocolo_aplicar_mensaje(&estado, linea)) {
                primer_estado = true;
            }
        }

        /* triggers de audio + animaciones segun eventos del servidor */
        if (strncmp(estado.ultimo_evento, ultimo_evento_audio, EVENTO_MAX) != 0) {
            if (strcmp(estado.ultimo_evento, "ALIEN_DESTROYED") == 0 ||
                strcmp(estado.ultimo_evento, "UFO_DESTROYED") == 0) {
                audio_explosion();
            } else if (strcmp(estado.ultimo_evento, "PLAYER_HIT") == 0 ||
                       strcmp(estado.ultimo_evento, "PLAYER_LIFE_LOST") == 0 ||
                       strcmp(estado.ultimo_evento, "PLAYER_ELIMINATED") == 0) {
                audio_vida_perdida();
                /* ultimo_evento_detalle viene como "id=<jugador>" si el
                 * payload trae el id; le quito el prefijo y disparo el
                 * shake + tinte rojo del canon afectado. */
                const char *detalle = estado.ultimo_evento_detalle;
                if (strncmp(detalle, "id=", 3) == 0) {
                    render_canon_hit(detalle + 3);
                }
            }
            snprintf(ultimo_evento_audio, sizeof(ultimo_evento_audio), "%s",
                     estado.ultimo_evento);
        }
        /* OVNI: cuando aparece (transicion ausente -> presente), suena. */
        if (estado.ovni_presente && !ovni_presente_audio) {
            audio_ovni();
        }
        ovni_presente_audio = estado.ovni_presente;

        /* enviar input solo si soy jugador */
        if (!spectator) {
            /* En GAME_OVER el unico input util es R (reiniciar partida).
             * El resto de comandos se filtran para evitar mandar inputs
             * sin sentido al servidor mientras esperamos el reinicio. */
            if (estado.juego_terminado) {
                if (input_reinicio_solicitado()) {
                    n = protocolo_construir_input(buf, sizeof(buf), id,
                                                  ACCION_REINICIAR);
                    if (n > 0 && !red_enviar(&con, buf, n)) {
                        con.conectado = false;
                    }
                }
            } else {
                const char *cmd = input_leer_comando_con_pico(&pico);
                if (cmd != NULL) {
                    /* Sonido de disparo: local al cliente (no espera al server). */
                    if (strcmp(cmd, ACCION_DISPARO) == 0) {
                        audio_disparo();
                    }
                    n = protocolo_construir_input(buf, sizeof(buf), id, cmd);
                    if (n > 0) {
                        /* si falla el envio, marcamos para salir del loop */
                        if (!red_enviar(&con, buf, n)) {
                            con.conectado = false;
                        }
                    }
                }
            }
        }

        /* render */
        if (primer_estado) {
            render_dibujar(&estado, id, target_watch, spectator ? 1 : 0);
        } else {
            render_dibujar_esperando(host, puerto);
        }
    }

    /* Si salimos por senial, dejar rastro en stderr antes del cleanup. */
    if (solicitud_salir) {
        fprintf(stderr, "senial recibida, cerrando...\n");
    }

    /* Si salimos porque el servidor cerro la conexion (no por el usuario),
     * drenamos cualquier mensaje pendiente (ERROR, eventos finales) para
     * tener el motivo del corte, y mostramos la pantalla de "desconectado"
     * con el detalle hasta que el usuario cierre la ventana manualmente.
     * Cubre el caso "espectador conecta antes que el jugador": el servidor
     * responde con ERROR + cierre y antes la ventana desaparecia sin
     * explicacion. */
    if (!con.conectado && !solicitud_salir) {
        while (red_recibir_linea(&con, linea, sizeof(linea))) {
            protocolo_aplicar_mensaje(&estado, linea);
        }
        char mensaje_corte[NOMBRE_MAX + 32];
        if (strcmp(estado.ultimo_evento, "ERROR") == 0
                && estado.ultimo_evento_detalle[0] != '\0') {
            snprintf(mensaje_corte, sizeof(mensaje_corte),
                     "ERROR: %s", estado.ultimo_evento_detalle);
        } else {
            snprintf(mensaje_corte, sizeof(mensaje_corte),
                     "Desconectado del servidor");
        }
        fprintf(stderr, "%s\n", mensaje_corte);
        /* do-while para garantizar al menos un frame dibujado. El cierre
         * se dispara con SIGINT, click en la X, o ESC. */
        do {
            render_dibujar_desconectado(mensaje_corte);
        } while (!input_quiere_salir() && !solicitud_salir);
    }

    /* --- despedida --- */
    if (con.conectado) {
        n = protocolo_construir_disconnect(buf, sizeof(buf), id);
        if (n > 0) {
            red_enviar(&con, buf, n);
        }
    }
    red_cerrar(&con);
    pico_cerrar(&pico);
    audio_cerrar();
    render_cerrar();

    printf("cliente cerrado\n");
    return 0;
}
