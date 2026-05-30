# client-c — Cliente jugador (C + raylib)

Cliente grafico del jugador en C imperativo usando raylib.
Conecta por TCP al servidor Java (`server-java/`) y envia inputs.
Opcionalmente lee comandos del Raspberry Pi Pico (`pico-controller/`) por UART.

## Estado
Pendiente de implementar (Fase 3 del plan).

## Estructura prevista
- `include/` — headers (`constants.h`, `structs.h`, `network.h`, `render.h`, `input.h`, ...).
- `src/` — implementaciones (`main.c`, `network.c`, `render.c`, `input.c`, ...).
- `assets/` — sprites y sonidos.
- `Makefile` — build con `gcc + -lraylib`.

## Build (cuando este implementado)
```bash
make
./client
```
