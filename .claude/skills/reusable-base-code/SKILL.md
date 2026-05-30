---
name: reusable-base-code
description: Documentación del código base reutilizable extraído del proyecto previo DonCEyKongJr. Consultar SIEMPRE al iniciar la implementación del servidor Java para verificar si existe una versión ya escrita y probada del archivo que se va a crear. Indica qué archivos copiar tal cual, cuáles adaptar, y cuáles ignorar. Ahorra reescribir infraestructura (sockets, patrón Observer, JSON, logger, game loop) que ya está probada y solo necesita cambio de paquete a cr.ac.tec.spaceinvaders.server.
---

# Código base reutilizable

> **Fuente:** Proyecto previo DonCEyKongJr (Donkey Kong Jr, 2025). Comparte arquitectura cliente-servidor TCP con el proyecto actual spaCEinvaders.

## Cuándo consultar esta skill

Antes de implementar **cualquier archivo** del servidor Java, verificar primero si existe una versión reutilizable. Esto aplica especialmente a:

- Servidor TCP, manejo de clientes, patrón Observer, JSON parsing.
- Logger, Config, GameLoop.
- Estructura de paquetes y build system Gradle.

Para el cliente C y el Pico, **NO hay código reutilizable** — implementar desde cero siguiendo las skills `c-client/SKILL.md` y `pico-controller/SKILL.md`.

## Archivos disponibles para copiar tal cual

Ubicación: `analisis-codigo-base/reutilizable/servidor-java/`

| Archivo | Acción | Líneas | Notas |
|---------|--------|--------|-------|
| `patrones/Observer.java` | Copiar tal cual | 14 | Interfaz del patrón |
| `patrones/Subject.java` | Copiar tal cual | 50 | Clase base, thread-safe con CopyOnWriteArrayList |
| `network/Mensaje.java` | Copiar tal cual | 116 | DTO con enum TipoMensaje |
| `network/JsonUtil.java` | Copiar tal cual | 93 | Wrapper Gson |
| `network/ServidorJuego.java` | Copiar tal cual | 98 | Acepta conexiones TCP |
| `network/ManejadorCliente.java` | Copiar tal cual | 278 | Maneja un cliente con threading + Observer |
| `util/LoggerUtil.java` | Copiar tal cual | 274 | Logger con timestamps y niveles |
| `eventos/EventoJuego.java` | Copiar (adaptado) | 60 | TipoEvento ya adaptado a Space Invaders |
| `util/Config.java` | Copiar (adaptado) | 200+ | Constantes adaptadas a Space Invaders |
| `build/build.gradle` | Copiar (simplificado) | 80 | Build con Gradle 21 + Gson |
| `build/settings.gradle.kts` | Copiar (renombrado) | 1 | Nombre del proyecto |

Todos los archivos ya tienen el package correcto: `cr.ac.tec.spaceinvaders.server.*`

## Cómo usar los archivos reutilizables

1. **Antes de implementar un archivo nuevo**, buscar si ya existe versión en `analisis-codigo-base/reutilizable/`.
2. Si existe, **copiar tal cual al proyecto** sin modificar.
3. Si no existe, implementar desde cero siguiendo las skills correspondientes.

## Archivos que SÍ deben implementarse desde cero

Estos no tienen versión reutilizable o deben reescribirse:

### Servidor Java
- `logic/GameManager.java` — Lógica del juego (reescribir 100%)
- `logic/entidades/*.java` — Todas las entidades (Alien, Squid, Crab, Octopus, UFO, Cannon, Bunker, Bullet, Player)
- `logic/patrones/AlienFactory.java` — Factory específico (inspirarse en `FactoryEntidad.java` del original)
- `structures/LinkedList.java` y `Node.java` — Lista enlazada propia (requisito del enunciado)
- `cli/ConsolaAdmin.java` — Comandos del enunciado (`Crear`, `OVNI`, `Velocidad`, `Bunkers`)
- `Main.java` — Versión simplificada sin AdminGUI

### Cliente C
- **TODO** desde cero. El cliente original es Windows-only (WinAPI) y no es portable.
- Usar **raylib** como librería gráfica.

### Pico
- **TODO** desde cero. No existe en el proyecto base.

## Reglas críticas al integrar

1. **NO copiar el cliente C del proyecto base.** Está atado a WinAPI (Windows-only) y los sprites son de Donkey Kong.
2. **NO copiar entidades del proyecto base.** Cocodrilos, frutas y lianas no aplican a Space Invaders.
3. **SÍ copiar toda la infraestructura de red.** Es código probado y resuelve la parte más difícil.
4. **Documentar la reutilización en la bitácora.** El profesor preguntará si código se ve muy pulido. Declarar la reutilización del proyecto propio del equipo evita problemas de "indicio de copia".

## Documentos relacionados

- `analisis-codigo-base/ANALISIS.md` — Análisis detallado completo
- `analisis-codigo-base/INTEGRATION.md` — Guía paso a paso de integración

## Validación de la integración

Después de copiar los archivos reutilizables, el proyecto debe compilar con:

```bash
cd server-java
./gradlew build
```

Si falla, lo más probable es que falte un stub de `GameManager`. Crear stub temporal con métodos vacíos antes de implementar la lógica real (ver `INTEGRATION.md` paso 3).
