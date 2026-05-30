# pico-controller — Firmware Raspberry Pi Pico

Control fisico de 2 botones para spaCEinvaders. Envia 1 byte ASCII por evento al
cliente C via UART (`/dev/ttyACM0` en Linux, vista a traves de un adaptador
USB-serial que recibe la TX del Pico).

## Hardware

| GPIO | Funcion                                                          |
|------|------------------------------------------------------------------|
| GP2  | Boton 1 (movimiento): 1 toque = izquierda, 2 toques = derecha    |
| GP3  | Boton 2: disparo                                                 |
| GP0  | UART0 TX -> conecta al RX del adaptador USB-serial del PC        |
| GP1  | UART0 RX (no usado)                                              |
| GND  | Comun con los botones y con el GND del adaptador USB-serial      |

Botones a GND con pull-up interno del Pico (no requieren resistencia externa).
Cableado:

```
   Pico GP2 ----[ boton 1 ]---- GND
   Pico GP3 ----[ boton 2 ]---- GND
   Pico GP0 (TX) ---------------> RX del adaptador USB-serial
   Pico GND  -------------------- GND del adaptador USB-serial
```

## Build

Requisitos:
- ARM GNU Toolchain (`arm-none-eabi-gcc`).
- CMake >= 3.13.
- Pico SDK >= 2.0.0 con variable `PICO_SDK_PATH` exportada.

```bash
export PICO_SDK_PATH=$HOME/.local/pico-sdk
export PATH=$HOME/.local/cmake/bin:$HOME/.local/arm-none-eabi/bin:$PATH

cd pico-controller
mkdir -p build && cd build
cmake .. -DCMAKE_BUILD_TYPE=Release
make -j$(nproc)
```

El artefacto queda en `build/controller.uf2`.

## Flashear

1. Conecta el Pico en modo BOOTSEL (manteniendo el boton BOOTSEL mientras lo enchufas por USB).
2. Aparecera como dispositivo USB de almacenamiento masivo.
3. Copia `controller.uf2` al volumen montado.
4. El Pico reinicia y arranca el firmware.

## Protocolo emitido

| Evento fisico                                | Byte enviado por UART |
|----------------------------------------------|-----------------------|
| 1 toque boton 1                              | `L` (0x4C)            |
| 2 toques boton 1 dentro de 300 ms            | `R` (0x52)            |
| Cada presion boton 2                         | `F` (0x46)            |

Sin terminadores. El cliente C lee byte por byte con `read()` no bloqueante.

## Constantes ajustables (`include/config.h`)

- `VENTANA_DOBLE_TOQUE_MS` (300): ventana para detectar 2 toques.
- `DEBOUNCE_MS` (20): umbral antirebote.
- `PIN_BOTON_MOVIMIENTO`, `PIN_BOTON_DISPARO`: pines GPIO si cambia el cableado.
- `UART_BAUDRATE` (115200): debe coincidir con el cliente C (`pico.c`).

## Decisiones de diseno

- **Polling, no interrupciones**: el loop principal corre con `sleep_ms(1)` entre
  iteraciones; con DEBOUNCE_MS = 20 ms y VENTANA = 300 ms hay margen sobrado para
  detectar ambos toques sin perder eventos. Las IRQ complicarian la maquina de
  estados sin beneficio real para 2 botones.
- **Pull-up interno**: simplifica el cableado (no requiere resistencia externa).
  Convencion: nivel alto = suelto, nivel bajo = presionado.
- **Ventana 1 vs 2 toques**: tras un primer flanco se entra a
  `ESPERANDO_SEGUNDO`. Si llega un segundo flanco antes de
  VENTANA_DOBLE_TOQUE_MS, se emite `R`. Si vence la ventana sin segundo flanco,
  se emite `L`. Esto implica una latencia maxima de ~300 ms para `L`, que es lo
  esperado dado el requisito de detectar 2 toques.
- **stdio USB/UART deshabilitados**: el UART0 se usa exclusivamente para enviar
  los bytes del protocolo; no se mezcla con printf de debug. Para depurar se
  puede habilitar temporalmente `pico_enable_stdio_usb(controller 1)` en el
  `CMakeLists.txt`, pero ojo: el cliente C abre `/dev/ttyACM0` y veria los
  printf como ruido.
