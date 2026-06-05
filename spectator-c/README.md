# spectator-c — Modo espectador del cliente C

El espectador **no es un binario separado**. Esta carpeta se conserva solo como
marcador historico; la funcionalidad esta implementada como un modo del cliente
unificado en `client-c/`, que produce el binario `client-c/build/client`.

## Como ejecutarlo

Compilar el cliente una sola vez (ver `client-c/README.md`) y luego correr el
binario con el flag `--spectator` mas `--watch <jugadorId>`:

```bash
./client --spectator --watch p1 --host 127.0.0.1 --port 5555
```

Flags relevantes (definidos en `client-c/src/main.c`):

- `--spectator` — activa el modo observador (no envia INPUT).
- `--watch <id>` — id del jugador a observar (obligatorio si se usa `--spectator`).
- `--host <ip>` — IP del servidor (default definido en `constants.h`).
- `--port <n>` — puerto TCP del servidor.
- `--id <id>` — id propio del espectador para el handshake.
- `--pico [device]` — se ignora en modo espectador (un espectador no manda input).

Si se pasa `--spectator` sin `--watch`, el cliente aborta antes de abrir el
socket para no gastar slots en el servidor.

## Limite de espectadores

El servidor Java acepta **1 o mas espectadores por jugador**. El tope esta
centralizado en `server-java/.../util/Config.java`:

```java
public static final int MAX_ESPECTADORES_POR_JUGADOR = 2;
```

Para subir o bajar el limite basta con cambiar esa constante y recompilar el
servidor; el cliente no necesita ajustes.

## Decision de diseno: un binario, dos modos

Se descarto mantener un proyecto C separado para el espectador. Ambos modos
comparten render, capa de red, parser de protocolo y manejo de estado, por lo
que un solo binario con un flag evita duplicar codigo, asegura que jugador y
espectador rendericen exactamente igual, y reduce la superficie a mantener y
testear.
