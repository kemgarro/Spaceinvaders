# spaCEinvaders

Implementación distribuida del clásico Space Invaders para el curso Paradigmas de Programación / Lenguajes, Compiladores e Intérpretes (Instituto Tecnológico de Costa Rica). Entrega: 11 de junio de 2026.

- **Servidor en Java** (orientado a objetos) — mantiene toda la lógica del juego, expone TCP en puerto 5555 con protocolo JSON.
- **Cliente jugador en C** (imperativo, raylib) — renderiza el estado y envía inputs.
- **Cliente espectador** — mismo binario que el jugador, con flag `--spectator`.
- **Cliente administrador GUI** (Java Swing) — envía comandos del enunciado (Crear / OVNI / Velocidad / Bunkers) al servidor por TCP.
- **Firmware Raspberry Pi Pico** (C, no requerido para entrega — ver nota al final).

---

## Componentes

| Carpeta | Contenido |
|---|---|
| `server-java/` | Servidor Java + admin GUI Swing (mismo jar) |
| `client-c/` | Cliente jugador/espectador en C con raylib |
| `pico-controller/` | Firmware del Pico (opcional) |
| `docs/` | Protocolo, diagramas UML, rúbrica |
| `bitacora/` | Registro diario del equipo |

---

## Requisitos de instalación

### 🐧 Linux (Ubuntu / Debian)

```bash
# Compilador C, herramientas de build, git
sudo apt update
sudo apt install -y build-essential git cmake pkg-config

# JDK 21 (para servidor + admin GUI)
sudo apt install -y openjdk-21-jdk

# Dependencias de raylib (cliente C)
sudo apt install -y libgl1-mesa-dev libx11-dev libxrandr-dev libxinerama-dev \
                    libxcursor-dev libxi-dev libasound2-dev
```

Después instalar **raylib 5.x** desde fuente (no está en apt en versión actualizada):

```bash
git clone --depth 1 --branch 5.0 https://github.com/raysan5/raylib.git ~/raylib-src
cd ~/raylib-src/src
make PLATFORM=PLATFORM_DESKTOP
# Instalación local en ~/.local (no requiere sudo):
make install RAYLIB_INSTALL_PATH=$HOME/.local/lib \
             RAYLIB_H_INSTALL_PATH=$HOME/.local/include
cd -
```

> El `Makefile` del cliente C asume que raylib quedó en `~/.local/`. Si lo instalás en otro lado (ej. `/usr/local`), editá `client-c/Makefile` (líneas `CFLAGS` y `LDLIBS`).

### 🪟 Windows

La forma más simple es **WSL2 con Ubuntu** y seguir las instrucciones de Linux arriba. Eso es lo que recomiendo.

Si querés instalación nativa Windows (sin WSL):

1. **Git for Windows**: https://git-scm.com/download/win
2. **JDK 21**: https://adoptium.net/temurin/releases/?version=21
3. **MSYS2** (compilador C + make): https://www.msys2.org/
   - En la terminal MSYS2 (MINGW64):
     ```bash
     pacman -S mingw-w64-x86_64-gcc mingw-w64-x86_64-make \
               mingw-w64-x86_64-raylib mingw-w64-x86_64-cmake \
               mingw-w64-x86_64-pkg-config
     ```
   - Agregar `C:\msys64\mingw64\bin` al PATH del sistema.
4. **Modificar `client-c/Makefile`**: cambiar los paths `$(HOME)/.local/...` por las rutas de MinGW (ej. `C:/msys64/mingw64/`).

> Para la entrega académica, **se recomienda Linux o WSL**: el cliente C está probado en esa plataforma y el script de build asume rutas POSIX.

---

## Clonar el repositorio

```bash
git clone https://github.com/kemgarro/Spaceinvaders.git
cd Spaceinvaders
```

---

## Compilar

### 1. Servidor Java + Admin GUI

```bash
cd server-java
./gradlew build
```

Esto:
- Descarga las dependencias (Gson) automáticamente vía Gradle wrapper.
- Compila el código.
- Corre los 126 tests unitarios.
- Genera el jar **fat (incluye dependencias)** en `server-java/build/libs/spaceinvaders-server-1.0.0.jar`.

### 2. Cliente C (jugador y espectador)

```bash
cd client-c
make
```

Genera el binario `client-c/build/client` (~1.6 MB, raylib estáticamente linkeada).

---

## Cómo correr

Necesitás **al menos 2 terminales**: una para el servidor y una para cada cliente.

### Terminal 1 — Servidor

```bash
cd server-java
java -jar build/libs/spaceinvaders-server-1.0.0.jar
```

El servidor escucha en `0.0.0.0:5555` y muestra una consola de admin en la misma terminal (escribí `ayuda` para ver los comandos).

### Terminal 2 — Jugador 1

```bash
cd client-c
./build/client --host 127.0.0.1 --port 5555 --id p1
```

Una ventana raylib abre con un splash. Apretá **ESPACIO** para conectar y empezar.

**Controles del jugador:**
- `← →` o `A D`: mover cañón
- `ESPACIO`: disparar
- `R`: reiniciar partida tras GAME OVER
- `ESC`: salir

### Terminal 3 — Jugador 2 (multijugador)

```bash
cd client-c
./build/client --host 127.0.0.1 --port 5555 --id p2
```

Igual que p1. Si los dos jugadores están en máquinas distintas, reemplazar `127.0.0.1` por la IP del servidor.

### Terminal 4 — Espectador de p1

```bash
cd client-c
./build/client --spectator --watch p1 --id e1 --host 127.0.0.1 --port 5555
```

El espectador observa lo que ve p1 sin poder mover ni disparar.

### Terminal 5 — Admin GUI (Swing)

```bash
cd server-java
java -cp build/libs/spaceinvaders-server-1.0.0.jar \
     cr.ac.tec.spaceinvaders.admin.AdminApp \
     --host 127.0.0.1 --port 5555 --id admin
```

Abre una ventana Swing con:
- Conectar al servidor (botón).
- Crear alien en (X, Y) con puntaje 10/20/40.
- Lanzar OVNI izquierda↔derecha con puntos base.
- Cambiar velocidad de aliens (ms entre pasos).
- Fijar salud de bunkers (0–100%).

---

## Flujo recomendado para probar

1. Levantar el **servidor** (terminal 1).
2. Levantar **p1** (terminal 2) → ESPACIO en su ventana → empieza el juego.
3. (Opcional) Levantar **p2** (terminal 3) → ESPACIO → multijugador.
4. (Opcional) Levantar **espectador** (terminal 4) → ESPACIO → solo observa.
5. (Opcional) Levantar **admin GUI** (terminal 5) → "Conectar" → modificar la partida en vivo.

> El **espectador siempre se conecta DESPUÉS del jugador objetivo**; si entra antes, el servidor lo rechaza con un overlay "ERROR: No hay partidas activas para observar".

---

## Documentación

- **`docs/protocolo.md`** — Especificación completa del protocolo JSON line-delimited.
- **`docs/diagramas/`** — Diagramas UML, arquitectura, estados y secuencia de mensajes (PDF generado de LaTeX).
- **`docs/grading-rubric.md`** — Rúbrica de evaluación del proyecto.
- **`bitacora/`** — Registro diario de actividades del equipo (RNF-17).

---

## Tests

```bash
cd server-java
./gradlew test
```

126 tests unitarios cubren entidades, motor, observer, factory, admin commands, JSON, espectadores, restart, motor pause.

---

## Nota sobre el Pico (opcional)

El proyecto incluye un firmware para Raspberry Pi Pico (`pico-controller/`) que actúa como control físico (2 botones por USB-CDC) y se conecta al cliente C con `--pico /dev/ttyACM0`. El firmware compilado (`.uf2`) ya está incluido en `pico-controller/build/`. Para recompilarlo necesitás el [Pico SDK](https://github.com/raspberrypi/pico-sdk) instalado, lo cual está fuera del alcance de esta guía.

Si no tenés el hardware del Pico, **todos los componentes (servidor + cliente + admin) funcionan perfectamente con teclado**.

---

## Estructura de ramas en GitHub

| Rama | Contenido |
|---|---|
| `main` | Versión integrada con todas las features. **Es la que hay que clonar para evaluar.** |
| `pico-usb-cdc-y-blink-test`, `fase6-pulido`, `admin-gui`, `pulido-visual`, `restart-game`, `motor-pause`, `disconnect-overlay`, `audit-fixes` | Branches por feature/autor, mantenidas como referencia histórica de quién contribuyó qué. |

---

## Equipo

3 integrantes. Distribución de responsabilidades documentada en `bitacora/`.
