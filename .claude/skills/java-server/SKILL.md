---
name: java-server
description: Guía específica para implementar el servidor Java del proyecto spaCEinvaders. Consultar al escribir cualquier código Java, al diseñar clases, al implementar patrones de diseño (Observer, Factory, Adapter), al manejar concurrencia entre threads de clientes, o al diseñar el modelo orientado a objetos. Incluye estructura de paquetes, plantillas de clases clave, implementación recomendada de los patrones requeridos, y manejo de threads para múltiples clientes simultáneos.
---

# Guía del servidor Java — spaCEinvaders

Esta skill cubre las decisiones de diseño, patrones, y convenciones del servidor Java. El servidor es el **cerebro del juego** — toda la lógica vive aquí.

## 1. Principios de diseño

### 1.1 Orientación a objetos auténtica
- **Encapsulamiento:** campos `private`, acceso por getters/setters explícitos solo cuando se necesite.
- **Herencia con propósito:** `Alien` abstracto → `Squid`, `Crab`, `Octopus` concretos. No herencia gratuita.
- **Polimorfismo:** método `getPoints()` y `getSprite()` virtual en `Alien`, sobreescrito en cada tipo.
- **Composición sobre herencia** cuando aplique (ej. `Player` *tiene* un `Cannon`, no *es* un `Cannon`).
- **Responsabilidad única:** cada clase hace una cosa bien.

### 1.2 Lo que NO es OOP
- ❌ Clases con solo métodos `static`.
- ❌ `main()` de 500 líneas haciendo toda la lógica.
- ❌ Pasar datos primitivos sueltos por todos lados en vez de objetos.
- ❌ Usar `public` en todo "por si acaso".

## 2. Patrones de diseño requeridos

El proyecto exige **mínimo 2 patrones** (Singleton NO cuenta). Recomendación: **Observer + Factory**.

### 2.1 Patrón Observer (recomendado #1)

**Por qué tiene sentido aquí:** Múltiples clientes (jugadores y espectadores) necesitan ser notificados cuando cambia el estado del juego. Es el caso de uso textbook para Observer.

**Implementación sugerida:**

```java
// Interfaz del observador
public interface GameObserver {
    void onGameEvent(GameEvent event);
}

// Sujeto observable
public class GameSubject {
    private final List<GameObserver> observers = new CopyOnWriteArrayList<>();
    
    public void addObserver(GameObserver obs) {
        observers.add(obs);
    }
    
    public void removeObserver(GameObserver obs) {
        observers.remove(obs);
    }
    
    protected void notifyAll(GameEvent event) {
        for (GameObserver obs : observers) {
            obs.onGameEvent(event);
        }
    }
}

// El GameEngine extiende GameSubject
public class GameEngine extends GameSubject {
    public void killAlien(int alienId, int killerPlayerId) {
        // ... lógica
        notifyAll(new AlienKillEvent(alienId, killerPlayerId));
    }
}

// Cada ClientHandler es un GameObserver
public class ClientHandler implements GameObserver {
    @Override
    public void onGameEvent(GameEvent event) {
        // Traduce el evento a mensaje del protocolo y lo envía por socket
        String msg = MessageBuilder.fromEvent(event);
        socket.writer.write(msg + "\n");
    }
}
```

**Uso de `CopyOnWriteArrayList`:** evita problemas de concurrencia cuando se agregan/quitan observers mientras se itera. Justificable en la defensa.

### 2.2 Patrón Factory (recomendado #2)

**Por qué tiene sentido aquí:** Necesitamos crear distintos tipos de `Alien` según un parámetro (los puntos: 10→Squid, 20→Crab, 40→Octopus). Esto es el caso típico de Factory Method.

**Implementación sugerida:**

```java
public class AlienFactory {
    
    public static Alien createAlien(int x, int y, int points) {
        return switch (points) {
            case 10 -> new Squid(x, y);
            case 20 -> new Crab(x, y);
            case 40 -> new Octopus(x, y);
            default -> throw new IllegalArgumentException(
                "Puntos inválidos para alien: " + points
            );
        };
    }
    
    public static Alien createAlienByType(int x, int y, AlienType type) {
        return switch (type) {
            case SQUID -> new Squid(x, y);
            case CRAB -> new Crab(x, y);
            case OCTOPUS -> new Octopus(x, y);
        };
    }
}
```

**¿Factory Method o Abstract Factory?**
- Lo de arriba es **Factory Method** (un método que crea objetos).
- **Abstract Factory** sería justificable si tuviéramos "familias" de productos relacionados (ej. una fábrica para "tema clásico" que crea Squid/Crab/Octopus clásicos, y otra para "tema futurista" que crea variantes futuristas). Esto agrega complejidad sin valor — quedarse con **Factory Method** y llamarlo correctamente en la documentación.

### 2.3 Patrón Adapter (opcional, como tercer patrón)

**Por qué podría tener sentido:** Si el protocolo de mensajes del enunciado (`Crear (X,Y,Pts)`) difiere del modelo interno, un `Adapter` puede traducir entre el formato externo y el interno.

```java
public class LegacyMessageAdapter implements MessageParser {
    public GameCommand parse(String legacyMessage) {
        // "Crear (1,1,1000)" → new CreateAlienCommand(1, 1, 1000)
        if (legacyMessage.startsWith("Crear")) {
            // ... parseo y conversión
        }
        // ...
    }
}
```

### 2.4 Singleton (justificable pero no cuenta)

Útil para `GameConfig` o `Logger`, pero **NO cuenta** como uno de los 2 patrones requeridos. Si se usa, agregar uno más.

## 3. Modelo de dominio (clases principales)

### 3.1 Diagrama de clases (texto)

```
                      ┌──────────────┐
                      │  GameSubject │ (abstract)
                      └──────┬───────┘
                             │
                      ┌──────▼───────┐
                      │  GameEngine  │
                      └──────┬───────┘
                             │ usa
              ┌──────────────┼──────────────┐
              │              │              │
         ┌────▼────┐   ┌─────▼─────┐   ┌────▼────┐
         │GameState│   │CollisionD.│   │WaveMgr  │
         └────┬────┘   └───────────┘   └─────────┘
              │
              │ contiene
    ┌─────────┼─────────┬─────────┬─────────┐
    │         │         │         │         │
┌───▼──┐ ┌────▼───┐ ┌───▼──┐ ┌────▼───┐ ┌───▼────┐
│Alien │ │ Cannon │ │Bunker│ │ Bullet │ │  UFO   │
└──┬───┘ └────────┘ └──────┘ └────────┘ └────────┘
   │ (abstract)
   ├────────┬────────┐
   │        │        │
┌──▼──┐ ┌───▼──┐ ┌───▼────┐
│Squid│ │ Crab │ │Octopus │
└─────┘ └──────┘ └────────┘
```

### 3.2 Clase `Alien` (ejemplo de jerarquía)

```java
public abstract class Alien {
    protected int id;
    protected int x, y;
    protected int width, height;
    protected boolean alive;
    
    public Alien(int id, int x, int y) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.alive = true;
    }
    
    public abstract int getPoints();
    public abstract AlienType getType();
    
    public void move(int dx, int dy) { this.x += dx; this.y += dy; }
    public void destroy() { this.alive = false; }
    public boolean isAlive() { return alive; }
    public Rectangle getBounds() { return new Rectangle(x, y, width, height); }
    
    // getters omitidos por brevedad
}

public class Squid extends Alien {
    public Squid(int id, int x, int y) {
        super(id, x, y);
        this.width = 24;
        this.height = 24;
    }
    @Override public int getPoints() { return 10; }
    @Override public AlienType getType() { return AlienType.SQUID; }
}

public class Crab extends Alien {
    public Crab(int id, int x, int y) {
        super(id, x, y);
        this.width = 32;
        this.height = 24;
    }
    @Override public int getPoints() { return 20; }
    @Override public AlienType getType() { return AlienType.CRAB; }
}

public class Octopus extends Alien {
    public Octopus(int id, int x, int y) {
        super(id, x, y);
        this.width = 36;
        this.height = 24;
    }
    @Override public int getPoints() { return 40; }
    @Override public AlienType getType() { return AlienType.OCTOPUS; }
}
```

### 3.3 Clase `Player`

```java
public class Player {
    private final int id;
    private final String name;
    private int score;
    private int lives;
    private Cannon cannon;
    
    public Player(int id, String name) {
        this.id = id;
        this.name = name;
        this.score = 0;
        this.lives = 3; // Requisito: inicia con 3 vidas
    }
    
    public void addScore(int points) { this.score += points; }
    public void loseLife() { if (lives > 0) lives--; }
    public void gainLife() { this.lives++; } // Al limpiar oleada
    public boolean isAlive() { return lives > 0; }
    
    // getters omitidos
}
```

### 3.4 Lista enlazada propia (`structures/LinkedList`)

El proyecto exige usar **listas como estructura de datos**. Aunque Java tiene `LinkedList` y `ArrayList`, conviene implementar una propia para mostrar el dominio del concepto y poder documentarla.

```java
public class LinkedList<T> {
    private Node<T> head;
    private int size;
    
    public void add(T data) { /* ... */ }
    public T get(int index) { /* ... */ }
    public boolean remove(T data) { /* ... */ }
    public int size() { return size; }
    public Iterator<T> iterator() { /* ... */ }
}

public class Node<T> {
    T data;
    Node<T> next;
    
    public Node(T data) {
        this.data = data;
        this.next = null;
    }
}
```

> Si se prefiere usar `java.util.LinkedList`, documentar la decisión. Pero implementar la propia da puntos de calidad y cumple el requisito explícito de "Crear y manipular listas como estructuras de datos".

## 4. Concurrencia: manejo de múltiples clientes

El servidor debe soportar múltiples clientes simultáneos. Arquitectura recomendada:

```
┌─────────────────────────────────────┐
│         GameServer                  │
│  (acepta conexiones en puerto)      │
│                                     │
│  while(true) {                      │
│    Socket s = serverSocket.accept();│
│    new Thread(new ClientHandler(s)) │
│       .start();                     │
│  }                                  │
└─────────────────────────────────────┘
         │
         │ por cada cliente
         ▼
┌─────────────────────────────────────┐
│       ClientHandler (Thread)        │
│  - Lee mensajes del socket          │
│  - Es un GameObserver               │
│  - Escribe mensajes al socket       │
└─────────────────────────────────────┘
         │
         │ ambos comparten
         ▼
┌─────────────────────────────────────┐
│      GameEngine (Thread)            │
│  - Game loop a 60 FPS               │
│  - Actualiza GameState              │
│  - Notifica observers               │
└─────────────────────────────────────┘
```

### 4.1 Sincronización

- `GameState` es **estado compartido** → usar `synchronized` en métodos que modifican o leer/escribir colecciones thread-safe.
- Lista de observers: `CopyOnWriteArrayList` (escrituras raras, lecturas frecuentes).
- Cola de comandos del admin: `BlockingQueue<AdminCommand>`.

### 4.2 Game loop

```java
public class GameEngine extends GameSubject implements Runnable {
    private static final int FPS = 60;
    private static final long FRAME_TIME = 1000 / FPS;
    private boolean running = true;
    
    @Override
    public void run() {
        long lastTick = System.currentTimeMillis();
        while (running) {
            long now = System.currentTimeMillis();
            long delta = now - lastTick;
            
            if (delta >= FRAME_TIME) {
                update(delta);
                lastTick = now;
            } else {
                try { Thread.sleep(1); } catch (InterruptedException e) { 
                    Thread.currentThread().interrupt(); 
                    break; 
                }
            }
        }
    }
    
    private void update(long delta) {
        synchronized (gameState) {
            moveAliens(delta);
            moveBullets(delta);
            checkCollisions();
            checkWaveComplete();
            checkGameOver();
        }
    }
}
```

## 5. Estructura de paquetes (recap)

```
cr.ac.tec.spaceinvaders
├── Main                            # Punto de entrada
├── server.*                        # Sockets, ClientHandler, AdminConsole
├── model.*                         # Entidades del juego
├── engine.*                        # GameEngine, colisiones, oleadas
├── patterns.factory.*              # AlienFactory
├── patterns.observer.*             # GameObserver, GameSubject
├── network.*                       # Parsing y construcción de mensajes
├── structures.*                    # LinkedList propia
└── util.*                          # Logger, Config
```

## 6. Manejo de excepciones

Cada excepción no manejada cuesta -2 puntos. Lugares críticos:

| Excepción | Dónde puede ocurrir | Cómo manejar |
|-----------|--------------------|--------------| 
| `IOException` | Socket read/write | try/catch, loggear, desconectar cliente |
| `SocketException` | Cliente se desconectó abruptamente | Capturar específicamente, eliminar observer |
| `NumberFormatException` | Parseo de mensajes con números | Loggear, enviar ERROR al cliente |
| `IllegalArgumentException` | Datos inválidos (puntos no estándar) | Loggear, rechazar comando |
| `ConcurrentModificationException` | Iterar colección mientras se modifica | Usar colecciones concurrentes |
| `NullPointerException` | Mensaje malformado | Validar antes de usar |
| `InterruptedException` | Thread interrumpido | Restaurar interrupt flag y salir limpio |

Plantilla de try/catch para sockets:

```java
try {
    String line = reader.readLine();
    if (line == null) {
        // Cliente cerró conexión limpiamente
        cleanup();
        return;
    }
    processMessage(line);
} catch (SocketException e) {
    logger.info("Cliente desconectado: " + playerId);
    cleanup();
} catch (IOException e) {
    logger.warning("Error de I/O con cliente " + playerId + ": " + e.getMessage());
    cleanup();
}
```

## 7. Logging

Centralizar logs en `util.Logger`. Niveles:
- `INFO`: eventos normales (conexión, desconexión, oleada).
- `WARNING`: errores recuperables (mensaje malformado, etc.).
- `SEVERE`: errores graves (no debería pasar pero pasa).

NUNCA usar `System.out.println` directamente para logging serio.

## 8. Build

Recomendado usar **Maven** (estándar de la industria) o **Gradle**. Ejemplo `pom.xml` mínimo:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project>
    <modelVersion>4.0.0</modelVersion>
    <groupId>cr.ac.tec</groupId>
    <artifactId>spaceinvaders-server</artifactId>
    <version>1.0.0</version>
    <properties>
        <maven.compiler.source>17</maven.compiler.source>
        <maven.compiler.target>17</maven.compiler.target>
    </properties>
    <build>
        <plugins>
            <plugin>
                <artifactId>maven-jar-plugin</artifactId>
                <configuration>
                    <archive>
                        <manifest>
                            <mainClass>cr.ac.tec.spaceinvaders.Main</mainClass>
                        </manifest>
                    </archive>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

Comando de build: `mvn clean package` → genera `target/spaceinvaders-server-1.0.0.jar`.
Ejecutar: `java -jar target/spaceinvaders-server-1.0.0.jar`.

## 9. Checklist de calidad antes de marcar como "terminado"

- [ ] Cada clase tiene una responsabilidad clara y única.
- [ ] No hay clases con métodos solo estáticos (excepto `Main` y factory methods justificados).
- [ ] Los 2 patrones están claramente identificados y documentados.
- [ ] Excepciones manejadas en cada interacción con socket.
- [ ] Estado compartido sincronizado correctamente.
- [ ] No hay `System.out.println` en código de producción (solo en debug temporal).
- [ ] `private` por defecto, `public` solo donde se necesita.
- [ ] Constantes en clase `Constants` o archivo de propiedades, no mágicas en el código.
- [ ] Comentarios JavaDoc en clases y métodos públicos principales.
- [ ] El JAR ejecutable se genera correctamente.
