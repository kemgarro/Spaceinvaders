---
name: project-structure
description: Define cómo deben organizarse las carpetas y archivos del proyecto spaCEinvaders. Consultar antes de crear archivos nuevos, mover archivos, o iniciar un nuevo módulo. Incluye la estructura completa para servidor Java (con paquetes), cliente C (con separación de constantes y módulos), firmware del Pico, documentación y bitácora. Asegura que el proyecto se vea profesional y sea fácil de navegar para el profesor durante la revisión.
---

# Estructura de carpetas del proyecto spaCEinvaders

Toda nueva creación de archivos debe respetar esta estructura. Esto ayuda al profesor a navegar el proyecto y demuestra organización.

## Estructura raíz

```
spaCEinvaders/
├── CLAUDE.md                       # Punto de entrada para Claude Code
├── README.md                       # Descripción general del proyecto
├── .claude/
│   └── skills/                     # Skills internas (este folder)
├── server-java/                    # Servidor en Java
├── client-c/                       # Cliente jugador en C
├── spectator-c/                    # Cliente espectador en C (puede ser variante del cliente)
├── pico-controller/                # Firmware Raspberry Pi Pico
├── docs/                           # Documentación oficial
├── bitacora/                       # Bitácora digital
├── builds/                         # Ejecutables compilados (no versionar)
└── scripts/                        # Scripts de build/run
```

## 1. Servidor Java (`server-java/`)

```
server-java/
├── pom.xml                         # O build.gradle si usan Gradle
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── cr/ac/tec/spaceinvaders/
│   │   │       ├── Main.java                       # Punto de entrada
│   │   │       ├── server/
│   │   │       │   ├── GameServer.java             # Acepta conexiones
│   │   │       │   ├── ClientHandler.java          # Maneja un cliente
│   │   │       │   └── AdminConsole.java           # Interfaz admin
│   │   │       ├── model/
│   │   │       │   ├── GameState.java              # Estado central
│   │   │       │   ├── entities/
│   │   │       │   │   ├── Alien.java              # Clase abstracta
│   │   │       │   │   ├── Squid.java              # 10 pts
│   │   │       │   │   ├── Crab.java               # 20 pts
│   │   │       │   │   ├── Octopus.java            # 40 pts
│   │   │       │   │   ├── UFO.java                # OVNI
│   │   │       │   │   ├── Cannon.java
│   │   │       │   │   ├── Bunker.java
│   │   │       │   │   └── Bullet.java
│   │   │       │   └── Player.java                 # Jugador con vidas y puntaje
│   │   │       ├── engine/
│   │   │       │   ├── GameEngine.java             # Loop principal
│   │   │       │   ├── CollisionDetector.java
│   │   │       │   └── WaveManager.java            # Maneja oleadas
│   │   │       ├── patterns/
│   │   │       │   ├── factory/
│   │   │       │   │   └── AlienFactory.java       # Patrón Factory
│   │   │       │   └── observer/
│   │   │       │       ├── GameObserver.java       # Interfaz Observer
│   │   │       │       └── GameSubject.java
│   │   │       ├── network/
│   │   │       │   ├── MessageProtocol.java        # Constantes del protocolo
│   │   │       │   ├── MessageParser.java
│   │   │       │   └── MessageBuilder.java
│   │   │       ├── structures/
│   │   │       │   ├── LinkedList.java             # Lista propia (requisito)
│   │   │       │   └── Node.java
│   │   │       └── util/
│   │   │           ├── Logger.java
│   │   │           └── Config.java
│   │   └── resources/
│   │       └── config.properties                   # Puerto, etc.
│   └── test/
│       └── java/
│           └── cr/ac/tec/spaceinvaders/            # Tests si los hay
└── README.md
```

### Convenciones del paquete Java
- Paquete base: `cr.ac.tec.spaceinvaders`
- Subpaquetes por responsabilidad, NO por tipo de clase.
- Clases en `PascalCase`, métodos y variables en `camelCase`, constantes en `UPPER_SNAKE_CASE`.
- Una clase pública por archivo.

## 2. Cliente C (`client-c/`)

```
client-c/
├── Makefile                        # Build con make
├── include/
│   ├── constants.h                 # TODAS las constantes (requisito explícito)
│   ├── structs.h                   # Definiciones de structs (Alien, Bullet, etc.)
│   ├── network.h                   # API de comunicación con servidor
│   ├── render.h                    # API de renderizado
│   ├── input.h                     # API de input (teclado + UART del Pico)
│   ├── game_state.h                # Estado local del cliente
│   └── linked_list.h               # Lista enlazada para entidades
├── src/
│   ├── main.c                      # Punto de entrada y loop principal
│   ├── network.c                   # Implementación socket cliente
│   ├── render.c                    # Implementación renderizado (raylib)
│   ├── input.c                     # Lectura de teclado + UART
│   ├── game_state.c                # Actualización del estado local
│   ├── linked_list.c               # Implementación de listas
│   ├── message_parser.c            # Parser de mensajes del servidor
│   └── uart_reader.c               # Lectura serial del Pico
├── assets/
│   ├── sprites/                    # PNGs de aliens, cañón, etc.
│   └── sounds/                     # Sonidos opcionales
└── README.md
```

### Convenciones de C
- Todo lo que pueda ser constante va en `constants.h` (puerto del servidor, dimensiones, colores, FPS, tamaños de buffer, etc.).
- Los structs van en `structs.h` con `typedef` para evitar repetir `struct` en cada uso.
- Cada módulo tiene un `.h` con la API pública y un `.c` con la implementación.
- Funciones en `snake_case` (ej. `crear_alien`, `dibujar_bunker`).
- Macros en `UPPER_SNAKE_CASE`.
- Variables locales en `snake_case`.

## 3. Cliente Espectador (`spectator-c/`)

Puede ser:
- **Opción A:** Un proyecto separado con su propio Makefile, compartiendo código con el cliente vía symlinks o duplicación controlada.
- **Opción B:** El mismo binario del cliente C con un flag (`--spectator` o se detecta del servidor).

**Recomendado:** Opción B con un flag al iniciar. Es más simple de mantener.

Si se elige Opción A, replicar la estructura del `client-c/`.

## 4. Firmware del Pico (`pico-controller/`)

```
pico-controller/
├── CMakeLists.txt                  # Build con CMake (Pico SDK)
├── pico_sdk_import.cmake
├── include/
│   ├── config.h                    # GPIO de botones, baudios UART, etc.
│   └── button.h                    # API de manejo de botones
├── src/
│   ├── main.c                      # Loop principal del Pico
│   ├── button.c                    # Debounce y detección de toques
│   └── uart_sender.c               # Envío por UART
└── README.md
```

### Convenciones del Pico
- Constantes de hardware (pines, baudrates) en `config.h`.
- Funciones específicas de hardware aisladas en sus módulos.
- Loop principal limpio: leer botones → procesar → enviar.

## 5. Documentación (`docs/`)

```
docs/
├── manual-usuario.pdf              # Cómo ejecutar el programa
├── diseno.pdf                      # Documento de diseño con los 9 puntos
├── anexo-atributos.pdf             # Anexo DI + AC (5% adicional)
├── algoritmos.md                   # Descripción detallada de algoritmos
├── estructuras-datos.md            # Descripción de listas, nodos, etc.
├── problemas-encontrados.md        # Sección requerida
├── plan-actividades.md             # Por estudiante (requerido)
├── conclusiones.md
├── bibliografia.md
└── diagramas/
    ├── arquitectura.png            # Diagrama de bloques
    ├── clases-uml.png              # UML del servidor Java
    ├── flujo-mensajes.png          # Sequence diagram de mensajes
    ├── estados-juego.png           # Diagrama de estados
    └── circuito-pico.png           # Esquemático del Pico + botones
```

## 6. Bitácora (`bitacora/`)

```
bitacora/
├── README.md                       # Índice de la bitácora
├── 2026-05-17.md                   # Una entrada por día (formato YYYY-MM-DD)
├── 2026-05-18.md
└── ...
```

### Formato de cada entrada de bitácora
```markdown
# Bitácora — 17 de mayo de 2026

## Participantes
- Persona A, Persona B, Persona C

## Actividades realizadas
- [Persona A] Investigación sobre patrones Observer en Java (2h).
- [Persona B] Setup de raylib en Ubuntu (1.5h).
- [Persona C] Diagrama inicial de arquitectura (2h).

## Decisiones tomadas
- Se elige raylib sobre SDL2 por simplicidad.
- Protocolo de mensajes será texto plano separado por `|`.

## Problemas encontrados
- raylib no encontraba `libGL` en la VM. Se solucionó con `apt install libgl1-mesa-dev`.

## Siguiente sesión
- Definir todos los mensajes del protocolo.
- Crear esqueleto de clases Java.
```

## 7. Builds y scripts (`builds/`, `scripts/`)

```
builds/                             # Ignorar en git
├── server.jar
├── client
├── spectator
└── pico-firmware.uf2

scripts/
├── build-all.sh                    # Compila todo
├── run-server.sh
├── run-client.sh
└── flash-pico.sh
```

## 8. Archivos en la raíz

- `README.md` — descripción general, cómo compilar, cómo ejecutar.
- `.gitignore` — ignorar `builds/`, `*.class`, `*.o`, `target/`, etc.
- `CLAUDE.md` — instrucciones para Claude Code.
- `LICENSE` — opcional, pero buena práctica.

## 9. Convención de nombres de archivos

- **Java:** `PascalCase.java`
- **C:** `snake_case.c`, `snake_case.h`
- **Markdown:** `kebab-case.md`
- **Imágenes/assets:** `kebab-case.png`
- **Carpetas:** `kebab-case/` (excepto paquetes Java)

## 10. Qué NO versionar (`.gitignore`)

```
# Java
*.class
target/
.idea/
*.iml

# C
*.o
*.exe
client
spectator
builds/

# Pico
build/
*.uf2

# OS
.DS_Store
Thumbs.db

# Documentos temporales
*.tmp
*~
```
