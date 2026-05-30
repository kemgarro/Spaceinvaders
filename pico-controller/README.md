# pico-controller — Firmware Raspberry Pi Pico (C)

Control fisico con 2 botones fisicos:
- Boton 1: 1 toque = izquierda, 2 toques rapidos = derecha.
- Boton 2: disparo.

Envia 1 byte ASCII por evento por UART al cliente C: `L`, `R`, `F`.

## Estado
Pendiente de implementar (Fase 5).

## Estructura prevista
- `include/config.h`, `include/button.h`
- `src/main.c`, `src/button.c`, `src/uart_sender.c`
- `CMakeLists.txt` con Pico SDK.

## Build (cuando este implementado)
```bash
mkdir build && cd build
cmake ..
make
```
Producira `controller.uf2` para flashear en el Pico (modo BOOTSEL).
