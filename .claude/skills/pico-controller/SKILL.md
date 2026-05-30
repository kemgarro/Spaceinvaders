---
name: pico-controller
description: Guía específica para implementar el firmware del Raspberry Pi Pico que actúa como control físico del juego spaCEinvaders. Consultar al escribir código C del Pico, al configurar GPIO de botones, al implementar debounce de botones, al detectar la diferencia entre uno y dos toques, al configurar UART, o al integrar el Pico con el cliente C por puerto serial. Incluye configuración del Pico SDK, esquemático de hardware, plantilla de CMakeLists.txt, y técnicas para detectar 1 vs 2 toques sin bloquear.
---

# Guía del Pico — spaCEinvaders

El Pico funciona como **control físico de 2 botones** que envía comandos al cliente C por UART.

## 1. Comportamiento requerido (recap del enunciado)

- **Botón 1** (control de movimiento):
  - **1 toque** → enviar comando "izquierda" (`L`)
  - **2 toques** (rápidos) → enviar comando "derecha" (`R`)
- **Botón 2** (disparo):
  - Cada presión → enviar comando "disparo" (`F`)
- **Comunicación:** UART (recomendado) o I2C. **UART es más simple** cuando el otro lado es una PC con conector USB.

## 2. Hardware

### 2.1 Componentes
- 1× Raspberry Pi Pico (no necesita ser W).
- 2× Botones pulsadores (push button).
- 2× Resistencias de pull-down (10kΩ) — opcional si se usa el pull-up interno del Pico.
- Cables jumper.
- Cable USB micro-B (para alimentación y comunicación serial).

### 2.2 Esquemático

```
    Pico              Botones
    ──────            ───────
    GP2  ───┬───── Botón1 ───┬─── GND
            │                │
           [PU interno activado]
    
    GP3  ───┬───── Botón2 ───┬─── GND
            │                │
           [PU interno activado]
    
    GND  ───────────────────────── GND botones
```

> **Truco:** Usar el pull-up interno del Pico (configurado por software) elimina la necesidad de resistencias externas. Cuando el botón está presionado, el pin lee `0` (LOW); cuando está suelto, lee `1` (HIGH).

### 2.3 GPIO sugeridos
- **GP2** → Botón 1 (movimiento)
- **GP3** → Botón 2 (disparo)
- **GP0, GP1** → UART0 TX/RX (estos son los pines por default del UART0)

## 3. UART: Pico → PC

### 3.1 Sobre USB
El Pico tiene UART nativo a través de USB-CDC (Communications Device Class). Cuando se conecta por USB y se habilita `pico_enable_stdio_usb`, el Pico aparece en la PC como un dispositivo `/dev/ttyACM0` (Linux) o `COMx` (Windows).

**Ventajas:**
- No requiere convertidor USB-Serial externo.
- Alimentación y datos por el mismo cable.
- Detectable automáticamente.

### 3.2 Protocolo (1 byte ASCII por evento)

| Byte | Significado |
|------|-------------|
| `L` | 1 toque del botón 1 (izquierda) |
| `R` | 2 toques del botón 1 (derecha) |
| `F` | Botón 2 presionado (disparar) |

> No se envían terminadores ni encabezados — solo el byte. El cliente C lee bytes individuales.

## 4. Detección de 1 vs 2 toques (sin bloquear)

Este es el punto **más complicado** del Pico. La regla es:
- Si el botón se presiona y NO se vuelve a presionar en X ms → fue 1 toque → enviar `L`.
- Si se presiona y se vuelve a presionar en menos de X ms → fueron 2 toques → enviar `R`.

**Donde X ≈ 300-400 ms** (ajustable).

### 4.1 Máquina de estados

```
                    ┌──────────────┐
                    │    IDLE      │
                    └──────┬───────┘
                           │ botón presionado
                           ▼
                    ┌──────────────┐
                    │  WAIT_SECOND │
                    │  (timer X ms)│
                    └──┬────────┬──┘
            timer expira│        │botón presionado otra vez
                        ▼        ▼
                  ┌─────────┐ ┌─────────┐
                  │ Enviar L│ │ Enviar R│
                  └────┬────┘ └────┬────┘
                       │           │
                       └─────┬─────┘
                             ▼
                       (volver a IDLE)
```

### 4.2 Implementación no bloqueante con timer

```c
#include "pico/stdlib.h"
#include "hardware/uart.h"
#include "hardware/gpio.h"

#define BOTON_1_PIN          2
#define BOTON_2_PIN          3
#define UART_ID              uart0
#define UART_TX_PIN          0
#define UART_RX_PIN          1
#define BAUDRATE             115200

#define VENTANA_DOBLE_TOQUE_MS  300
#define DEBOUNCE_MS             20

typedef enum {
    BOTON1_IDLE,
    BOTON1_ESPERANDO_SEGUNDO
} EstadoBoton1;

static EstadoBoton1 estado_boton1 = BOTON1_IDLE;
static absolute_time_t tiempo_primer_toque;
static absolute_time_t ultimo_cambio_b1;
static absolute_time_t ultimo_cambio_b2;
static bool ultimo_estado_b1 = true;  /* pull-up: suelto = true */
static bool ultimo_estado_b2 = true;

void enviar_byte(char c) {
    uart_putc(UART_ID, c);
}

bool boton_presionado_con_debounce(uint pin, bool *ultimo_estado, 
                                    absolute_time_t *ultimo_cambio) {
    bool actual = gpio_get(pin);
    absolute_time_t ahora = get_absolute_time();
    
    if (actual != *ultimo_estado) {
        /* Verificar debounce */
        if (absolute_time_diff_us(*ultimo_cambio, ahora) > DEBOUNCE_MS * 1000) {
            *ultimo_estado = actual;
            *ultimo_cambio = ahora;
            /* Detectar flanco de bajada: true→false = presionado */
            if (actual == false) return true;
        }
    }
    return false;
}

void procesar_boton1(void) {
    bool fue_presionado = boton_presionado_con_debounce(
        BOTON_1_PIN, &ultimo_estado_b1, &ultimo_cambio_b1
    );
    
    switch (estado_boton1) {
        case BOTON1_IDLE:
            if (fue_presionado) {
                tiempo_primer_toque = get_absolute_time();
                estado_boton1 = BOTON1_ESPERANDO_SEGUNDO;
            }
            break;
            
        case BOTON1_ESPERANDO_SEGUNDO:
            if (fue_presionado) {
                /* Segundo toque dentro de la ventana */
                enviar_byte('R');
                estado_boton1 = BOTON1_IDLE;
            } else {
                /* Verificar si expiró la ventana */
                int64_t transcurrido = absolute_time_diff_us(
                    tiempo_primer_toque, get_absolute_time()
                );
                if (transcurrido > VENTANA_DOBLE_TOQUE_MS * 1000) {
                    enviar_byte('L');
                    estado_boton1 = BOTON1_IDLE;
                }
            }
            break;
    }
}

void procesar_boton2(void) {
    bool fue_presionado = boton_presionado_con_debounce(
        BOTON_2_PIN, &ultimo_estado_b2, &ultimo_cambio_b2
    );
    if (fue_presionado) {
        enviar_byte('F');
    }
}

int main() {
    /* Inicializar stdio sobre USB para debug */
    stdio_init_all();
    
    /* Configurar GPIO de botones con pull-up interno */
    gpio_init(BOTON_1_PIN);
    gpio_set_dir(BOTON_1_PIN, GPIO_IN);
    gpio_pull_up(BOTON_1_PIN);
    
    gpio_init(BOTON_2_PIN);
    gpio_set_dir(BOTON_2_PIN, GPIO_IN);
    gpio_pull_up(BOTON_2_PIN);
    
    /* Configurar UART */
    uart_init(UART_ID, BAUDRATE);
    gpio_set_function(UART_TX_PIN, GPIO_FUNC_UART);
    gpio_set_function(UART_RX_PIN, GPIO_FUNC_UART);
    
    /* Inicializar timers */
    ultimo_cambio_b1 = get_absolute_time();
    ultimo_cambio_b2 = get_absolute_time();
    
    /* Loop principal */
    while (true) {
        procesar_boton1();
        procesar_boton2();
        sleep_ms(1);  /* Pequeño respiro al CPU */
    }
    
    return 0;
}
```

## 5. Archivo `config.h`

Centralizar las constantes del Pico:

```c
#ifndef CONFIG_H
#define CONFIG_H

#define BOTON_1_PIN              2
#define BOTON_2_PIN              3
#define UART_ID                  uart0
#define UART_TX_PIN              0
#define UART_RX_PIN              1
#define BAUDRATE                 115200

#define VENTANA_DOBLE_TOQUE_MS   300
#define DEBOUNCE_MS              20

#define BYTE_IZQUIERDA           'L'
#define BYTE_DERECHA             'R'
#define BYTE_DISPARO             'F'

#endif
```

## 6. Build con Pico SDK

### 6.1 Estructura

```
pico-controller/
├── CMakeLists.txt
├── pico_sdk_import.cmake
├── include/
│   ├── config.h
│   └── button.h
└── src/
    ├── main.c
    └── button.c
```

### 6.2 `CMakeLists.txt`

```cmake
cmake_minimum_required(VERSION 3.13)

include(pico_sdk_import.cmake)

project(spaceinvaders_controller C CXX ASM)
set(CMAKE_C_STANDARD 11)

pico_sdk_init()

add_executable(controller
    src/main.c
    src/button.c
)

target_include_directories(controller PRIVATE include)

target_link_libraries(controller 
    pico_stdlib 
    hardware_uart 
    hardware_gpio
)

# Habilitar stdio sobre USB (para debug)
pico_enable_stdio_usb(controller 1)
pico_enable_stdio_uart(controller 0)

pico_add_extra_outputs(controller)
```

### 6.3 Compilar

```bash
mkdir build && cd build
cmake ..
make
```

Esto produce `controller.uf2`. Arrastrar al Pico montado en modo BOOTSEL.

## 7. Pruebas

### 7.1 Probar sin el cliente C
Con `minicom`, `screen` o `cu`:

```bash
sudo minicom -D /dev/ttyACM0 -b 115200
# o
screen /dev/ttyACM0 115200
```

Al presionar botones deberías ver `L`, `R`, o `F` en la terminal.

### 7.2 Probar la detección de 1 vs 2 toques
- Tap rápido (un solo press) → `L`
- Dos taps en menos de 300ms → `R`
- Probar el límite: tap, pausa de 500ms, tap → debería dar `LL` (dos izquierdas)

## 8. Problemas comunes y soluciones

| Problema | Causa probable | Solución |
|----------|---------------|----------|
| El Pico no aparece en `/dev/ttyACM0` | `pico_enable_stdio_usb` no habilitado | Verificar `CMakeLists.txt` |
| Lecturas erráticas de botones | Falta de debounce | Aumentar `DEBOUNCE_MS` a 30-50 |
| Toques fantasma | Cableado flojo | Verificar conexiones, agregar capacitor 100nF |
| 2 toques se detectan como 1 | Ventana muy corta | Aumentar `VENTANA_DOBLE_TOQUE_MS` |
| 1 toque se detecta como 2 | Botón con mucho rebote | Aumentar debounce, mejorar botón físico |
| El cliente C no recibe bytes | Puerto incorrecto en `constants.h` | Verificar con `ls /dev/ttyACM*` |
| Permisos negados al abrir UART | Usuario no en grupo `dialout` | `sudo usermod -aG dialout $USER` y relogin |

## 9. Checklist de entrega del Pico

- [ ] El `.uf2` compila y se puede flashear.
- [ ] Los 2 botones se detectan correctamente.
- [ ] 1 toque → envía `L` por UART.
- [ ] 2 toques rápidos → envía `R` por UART.
- [ ] Botón 2 → envía `F` por UART.
- [ ] El debounce evita lecturas dobles falsas.
- [ ] El cliente C recibe correctamente los bytes en `/dev/ttyACM0`.
- [ ] Las constantes (pines, baudrate, ventana) están en `config.h`.
- [ ] El código no usa `sleep_ms` largos que bloqueen detección.
- [ ] Foto/diagrama del cableado para incluir en la documentación.

## 10. Apartado de seguridad

- Verificar polaridad del cable USB.
- No conectar 5V a los GPIOs (son 3.3V).
- Si los botones tienen LED, asegurarse de que el LED no se alimente del GPIO.
