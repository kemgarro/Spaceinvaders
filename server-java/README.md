# Servidor Java — spaCEinvaders

## Estado actual

Este servidor contiene la **infraestructura** ya implementada (reusada del proyecto previo DonCEyKongJr, con paquetes y constantes adaptados a Space Invaders).

### ✅ Ya implementado y validado (compila con Java 21)

```
src/main/java/cr/ac/tec/spaceinvaders/server/
├── network/
│   ├── Mensaje.java           # DTO mensajes JSON con enum TipoMensaje
│   ├── JsonUtil.java          # Wrapper Gson + helpers
│   ├── ServidorJuego.java     # Servidor TCP multicliente
│   └── ManejadorCliente.java  # Un thread por cliente, distingue PLAYER/SPECTATOR
├── logic/
│   ├── patrones/
│   │   ├── Observer.java      # Interfaz Observer
│   │   └── Subject.java       # Base abstracta (CopyOnWriteArrayList)
│   └── eventos/
│       └── EventoJuego.java   # Evento con TipoEvento adaptado a SI
└── util/
    ├── LoggerUtil.java        # Logger con timestamps + niveles
    └── Config.java            # Constantes adaptadas a Space Invaders
```

### ❌ Falta implementar

```
src/main/java/cr/ac/tec/spaceinvaders/server/
├── Main.java                  # Punto de entrada (plantilla en INTEGRATION.md)
├── logic/
│   ├── GameManager.java       # Lógica central del juego — REESCRIBIR
│   ├── GameLoop.java          # Copiar del proyecto base con cambio de package
│   ├── entidades/
│   │   ├── Alien.java         # Abstracta
│   │   ├── Squid.java         # 10 pts
│   │   ├── Crab.java          # 20 pts
│   │   ├── Octopus.java       # 40 pts
│   │   ├── UFO.java
│   │   ├── Cannon.java
│   │   ├── Bunker.java
│   │   ├── Bullet.java
│   │   └── Player.java
│   └── patrones/
│       └── AlienFactory.java  # Factory para crear aliens
├── cli/
│   └── ConsolaAdmin.java      # Comandos del enunciado
└── structures/
    ├── LinkedList.java        # Lista propia (requisito)
    └── Node.java
```

## Compilar y ejecutar

```bash
# Compilar
./gradlew build

# Generar JAR ejecutable
./gradlew jar
# El JAR queda en build/libs/spaceinvaders-server-1.0.0.jar

# Ejecutar desde Gradle
./gradlew run

# Ejecutar JAR directamente
java -jar build/libs/spaceinvaders-server-1.0.0.jar [puerto]

# Por defecto el puerto es 5555
```

## Verificación de la integración

Antes de implementar el `GameManager` real, agregar este stub para que compile:

```java
// src/main/java/cr/ac/tec/spaceinvaders/server/logic/GameManager.java
package cr.ac.tec.spaceinvaders.server.logic;

import cr.ac.tec.spaceinvaders.server.logic.patrones.Subject;
import java.util.HashMap;
import java.util.Map;

public class GameManager extends Subject {
    public boolean tieneEspacio() { return true; }
    public boolean hayJugadorActivo() { return true; }
    public int contarJugadoresActivos() { return 0; }
    public boolean agregarJugador(String id, double x, double y, int extra) { return true; }
    public boolean registrarEspectador(String id) { return true; }
    public void eliminarJugador(String id) {}
    public void eliminarEspectador(String id) {}
    public void procesarInput(String id, String accion) {}
    public Map<String, Object> getEstadoJuego() { return new HashMap<>(); }
    public void actualizar(double deltaTime) {}
    public void shutdown() {}
}
```

Con este stub + un `Main.java` simple, debería arrancar y aceptar conexiones por sockets, aunque sin lógica de juego real.

## Origen del código reusado

Esta infraestructura proviene del proyecto **DonCEyKongJr** del mismo equipo (2025), con las siguientes modificaciones:

- Paquete cambiado de `cr.tec.donceykongjr.server` a `cr.ac.tec.spaceinvaders.server`.
- `Config.java` reescrito con constantes de Space Invaders (10/20/40 pts, 3 vidas, 4 bunkers, etc.).
- `EventoJuego.java` reescrito con tipos de eventos de Space Invaders.
- `build.gradle` simplificado (eliminadas dependencias innecesarias y tareas específicas del cliente C anterior).
- `Main.java`, `GameManager.java` y todo el directorio `entidades/` deben implementarse desde cero porque corresponden a la lógica específica de cada juego.

Ver `../analisis-codigo-base/ANALISIS.md` y `../analisis-codigo-base/INTEGRATION.md` para detalles.
