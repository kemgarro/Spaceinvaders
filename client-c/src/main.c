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

#include "constants.h"
#include "input.h"
#include "network.h"
#include "pico.h"
#include "protocol.h"
#include "render.h"

#define MAIN_ID_DEFAULT     "p1"
#define MAIN_BUF_MENSAJE    256

/* Bandera global para senializar SIGINT/SIGTERM al loop principal. */
static volatile sig_atomic_t solicitud_salir = 0;

/* Agrupa los argumentos de linea de comandos ya parseados y validados.
 * Es un struct local a main.c (no se expone en header) usado solo para
 * pasar contexto entre las funciones helper de este archivo. */
typedef struct {
    const char *host;
    int         puerto;
    const char *id;
    bool        spectator;
    const char *target_watch;
    /* NULL = sin Pico. No NULL = ruta del dispositivo serie. */
    const char *pico_device;
} OpcionesCliente;

/* Agrupa los recursos de runtime (socket, pico) para pasarlos al loop y
 * al cleanup como un solo bloque. Tambien es local a main.c. */
typedef struct {
    Conexion     con;
    ConexionPico pico;
} ContextoCliente;

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

/* Aplica defaults y valida coherencia de los flags ya parseados. Devuelve
 *   0 -> ok
 *   2 -> espectador sin --watch (caller debe return 2)
 * Tambien silencia flags incompatibles (avisando por stderr). */
static int validar_opciones(OpcionesCliente *opts) {
    /* --- validacion de flags --- */
    /* El espectador SIEMPRE necesita target; si no se pasa, salimos antes de
     * abrir el socket para evitar gastar slots en el servidor. */
    if (opts->spectator && (opts->target_watch == NULL || opts->target_watch[0] == '\0')) {
        fprintf(stderr, "modo espectador requiere --watch <id>\n");
        return 2;
    }
    /* En modo jugador --watch no tiene sentido: lo ignoramos pero avisamos. */
    if (!opts->spectator && opts->target_watch != NULL) {
        fprintf(stderr, "aviso: --watch se ignora en modo jugador\n");
        opts->target_watch = NULL;
    }
    /* En modo espectador --pico no tiene sentido: un espectador no manda input. */
    if (opts->spectator && opts->pico_device != NULL) {
        fprintf(stderr, "aviso: --pico se ignora en modo espectador\n");
        opts->pico_device = NULL;
    }
    return 0;
}

/* Parsea argv hacia OpcionesCliente. Devuelve:
 *   0  -> ok, continuar
 *   1  -> error de parseo o validacion (caller debe return 1)
 *   2  -> error de validacion de modo espectador (caller debe return 2)
 *  -1  -> el usuario pidio --help; caller debe return 0
 */
static int parsear_argumentos(int argc, char *argv[], OpcionesCliente *opts) {
    /* --- parseo de argumentos --- */
    opts->host = SERVIDOR_IP_DEFAULT;
    opts->puerto = SERVIDOR_PUERTO;
    opts->id = MAIN_ID_DEFAULT;
    opts->spectator = false;
    opts->target_watch = NULL;
    /* NULL = sin Pico. No NULL = ruta del dispositivo serie. */
    opts->pico_device = NULL;

    for (int i = 1; i < argc; i++) {
        if (strcmp(argv[i], "--host") == 0 && i + 1 < argc) {
            opts->host = argv[++i];
        } else if (strcmp(argv[i], "--port") == 0 && i + 1 < argc) {
            opts->puerto = atoi(argv[++i]);
        } else if (strcmp(argv[i], "--id") == 0 && i + 1 < argc) {
            opts->id = argv[++i];
        } else if (strcmp(argv[i], "--spectator") == 0) {
            opts->spectator = true;
        } else if (strcmp(argv[i], "--watch") == 0 && i + 1 < argc) {
            opts->target_watch = argv[++i];
        } else if (strcmp(argv[i], "--pico") == 0) {
            /* Argumento opcional: si lo que sigue no empieza con '-', se toma
             * como ruta del dispositivo; si no, se usa el default. */
            if (i + 1 < argc && argv[i + 1][0] != '-') {
                opts->pico_device = argv[++i];
            } else {
                opts->pico_device = UART_DISPOSITIVO_DEFAULT;
            }
        } else if (strcmp(argv[i], "--help") == 0 || strcmp(argv[i], "-h") == 0) {
            main_imprimir_uso(argv[0]);
            return -1;
        } else {
            fprintf(stderr, "argumento no reconocido: %s\n", argv[i]);
            main_imprimir_uso(argv[0]);
            return 1;
        }
    }

    return validar_opciones(opts);
}

/* Instala los handlers de SIGINT y SIGTERM que marcan la bandera de salida.
 * Lo dejamos antes de abrir el socket para que cualquier Ctrl+C durante
 * el handshake termine ordenado y no como SIGINT default. */
static void instalar_signal_handlers(void) {
    struct sigaction sa;
    memset(&sa, 0, sizeof(sa));
    sa.sa_handler = manejador_senales;
    sigemptyset(&sa.sa_mask);
    sigaction(SIGINT, &sa, NULL);
    sigaction(SIGTERM, &sa, NULL);
}

/* Abre el socket TCP y envia el CONNECT inicial. Devuelve true si todo
 * salio bien; en caso de fallo deja stderr con el motivo y cierra lo
 * que haya quedado abierto. */
static bool conectar_y_saludar(ContextoCliente *ctx, const OpcionesCliente *opts) {
    /* --- conexion al servidor --- */
    red_inicializar(&ctx->con);
    if (!red_conectar(&ctx->con, opts->host, opts->puerto)) {
        fprintf(stderr, "no se pudo conectar a %s:%d\n", opts->host, opts->puerto);
        return false;
    }

    const char *tipo_cliente = opts->spectator ? TIPO_SPECTATOR : TIPO_PLAYER;
    printf("cliente conectado como %s (%s)\n", opts->id, tipo_cliente);
    if (opts->spectator) {
        printf("observando a %s\n", opts->target_watch);
    }

    char buf[MAIN_BUF_MENSAJE];
    int n = protocolo_construir_connect(buf, sizeof(buf), opts->id, tipo_cliente, opts->target_watch);
    if (n <= 0 || !red_enviar(&ctx->con, buf, n)) {
        fprintf(stderr, "fallo al enviar CONNECT\n");
        red_cerrar(&ctx->con);
        return false;
    }
    return true;
}

/* Inicializa la conexion al Pico si corresponde (solo jugador y con --pico).
 * Si no se pidio Pico o falla la apertura, el cliente sigue funcionando
 * solo con teclado. */
static void inicializar_pico(ContextoCliente *ctx, const OpcionesCliente *opts) {
    /* --- inicializa Pico (opcional, solo si jugador y se paso --pico) --- */
    pico_inicializar(&ctx->pico);
    if (opts->pico_device != NULL && !opts->spectator) {
        if (pico_abrir(&ctx->pico, opts->pico_device)) {
            printf("pico: conectado a %s (%d baud)\n",
                   opts->pico_device, UART_BAUDRATE_DOC);
        } else {
            printf("pico: no se pudo abrir %s, sigo solo con teclado\n",
                   opts->pico_device);
        }
    }
}

/* Loop principal: drena mensajes del servidor, envia input local y renderiza.
 * Sale cuando el usuario cierra la ventana, se pierde la conexion o llega
 * una senial. */
static void loop_principal(ContextoCliente *ctx, const OpcionesCliente *opts) {
    EstadoVista estado;
    protocolo_estado_inicializar(&estado);

    bool primer_estado = false;
    char linea[BUFFER_RED];
    char buf[MAIN_BUF_MENSAJE];
    int n;

    /* --- loop principal --- */
    while (!input_quiere_salir() && ctx->con.conectado && !solicitud_salir) {
        /* drenamos todas las lineas pendientes del servidor */
        while (red_recibir_linea(&ctx->con, linea, sizeof(linea))) {
            if (protocolo_aplicar_mensaje(&estado, linea)) {
                primer_estado = true;
            }
        }

        /* enviar input solo si soy jugador */
        if (!opts->spectator) {
            const char *cmd = input_leer_comando_con_pico(&ctx->pico);
            if (cmd != NULL) {
                n = protocolo_construir_input(buf, sizeof(buf), opts->id, cmd);
                if (n > 0) {
                    /* si falla el envio, marcamos para salir del loop */
                    if (!red_enviar(&ctx->con, buf, n)) {
                        ctx->con.conectado = false;
                    }
                }
            }
        }

        /* render */
        if (primer_estado) {
            render_dibujar(&estado, opts->id, opts->target_watch, opts->spectator ? 1 : 0);
        } else {
            render_dibujar_esperando(opts->host, opts->puerto);
        }
    }
}

/* Cierre ordenado: envia DISCONNECT si la conexion sigue viva, cierra
 * socket, pico y la ventana de raylib. */
static void cleanup(ContextoCliente *ctx, const OpcionesCliente *opts) {
    char buf[MAIN_BUF_MENSAJE];
    int n;

    /* --- despedida --- */
    if (ctx->con.conectado) {
        n = protocolo_construir_disconnect(buf, sizeof(buf), opts->id);
        if (n > 0) {
            red_enviar(&ctx->con, buf, n);
        }
    }
    red_cerrar(&ctx->con);
    pico_cerrar(&ctx->pico);
    render_cerrar();
}

int main(int argc, char *argv[]) {
    OpcionesCliente opts;
    int rc = parsear_argumentos(argc, argv, &opts);
    if (rc == -1) {
        return 0;  /* --help */
    }
    if (rc != 0) {
        return rc;  /* 1 = error parseo, 2 = espectador sin --watch */
    }

    instalar_signal_handlers();

    ContextoCliente ctx;
    if (!conectar_y_saludar(&ctx, &opts)) {
        return 1;
    }

    /* --- inicializa render y estado --- */
    render_inicializar();

    inicializar_pico(&ctx, &opts);

    loop_principal(&ctx, &opts);

    /* Si salimos por senial, dejar rastro en stderr antes del cleanup. */
    if (solicitud_salir) {
        fprintf(stderr, "senial recibida, cerrando...\n");
    }

    cleanup(&ctx, &opts);

    printf("cliente cerrado\n");
    return 0;
}
