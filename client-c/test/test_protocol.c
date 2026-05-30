/*
 * test_protocol.c
 * ---------------
 * Prueba CLI end-to-end del cliente: abre TCP al servidor Java, manda CONNECT,
 * imprime un resumen de los primeros STATE recibidos, y manda DISCONNECT.
 * Sin raylib. Util para validar la capa de red + parser sin la UI.
 *
 * Uso:
 *   ./test_protocol [host] [puerto] [jugador_id]
 * Default: 127.0.0.1 5555 p1_test
 */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <time.h>

#include "constants.h"
#include "network.h"
#include "protocol.h"

int main(int argc, char *argv[]) {
    const char *host = (argc > 1) ? argv[1] : SERVIDOR_IP_DEFAULT;
    int puerto = (argc > 2) ? atoi(argv[2]) : SERVIDOR_PUERTO;
    const char *jugador_id = (argc > 3) ? argv[3] : "p1_test";

    Conexion con;
    red_inicializar(&con);
    if (!red_conectar(&con, host, puerto)) {
        fprintf(stderr, "no se pudo conectar a %s:%d\n", host, puerto);
        return 1;
    }
    printf("conectado a %s:%d como %s\n", host, puerto, jugador_id);

    /* enviar CONNECT */
    char buf[256];
    int n = protocolo_construir_connect(buf, sizeof(buf), jugador_id, TIPO_PLAYER, NULL);
    if (n < 0 || !red_enviar(&con, buf, n)) {
        fprintf(stderr, "fallo al enviar CONNECT\n");
        red_cerrar(&con);
        return 1;
    }
    printf("CONNECT enviado\n");

    EstadoVista estado;
    protocolo_estado_inicializar(&estado);

    /* esperar y procesar hasta N estados o timeout */
    char linea[BUFFER_RED];
    int estados_recibidos = 0;
    time_t inicio = time(NULL);
    while (estados_recibidos < 3 && difftime(time(NULL), inicio) < 5.0) {
        if (red_recibir_linea(&con, linea, sizeof(linea))) {
            if (protocolo_aplicar_mensaje(&estado, linea)) {
                estados_recibidos++;
                printf("[msg %d] aliens=%d balas=%d bunkers=%d canones=%d jugadores=%d ovni=%s oleada=%d ultimo_evento=%s\n",
                       estados_recibidos, estado.n_aliens, estado.n_balas, estado.n_bunkers,
                       estado.n_canones, estado.n_jugadores,
                       estado.ovni_presente ? "si" : "no",
                       estado.oleada,
                       estado.ultimo_evento[0] ? estado.ultimo_evento : "(ninguno)");
            }
        } else {
            usleep(10000); /* 10 ms */
        }
    }

    /* enviar DISCONNECT */
    n = protocolo_construir_disconnect(buf, sizeof(buf), jugador_id);
    if (n > 0) {
        red_enviar(&con, buf, n);
    }
    red_cerrar(&con);

    if (estados_recibidos == 0) {
        fprintf(stderr, "ningun STATE recibido\n");
        return 2;
    }
    printf("OK: %d estados recibidos\n", estados_recibidos);
    return 0;
}
