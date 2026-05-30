---
name: game-mechanics
description: Especificación completa de las reglas y mecánicas del juego Space Invaders tal como deben implementarse en este proyecto. Consultar al implementar lógica del juego, sistema de puntuación, movimiento de aliens, comportamiento del OVNI, bunkers, vidas, colisiones, oleadas, condiciones de game over, o cualquier comportamiento del juego. Incluye valores exactos requeridos por el enunciado y comportamientos derivados del Space Invaders original.
---

# Mecánicas del juego spaCEinvaders

Especificación funcional de cómo debe comportarse el juego. Todas las decisiones de gameplay deben basarse en este documento.

## 1. Entidades del juego

### 1.1 Cañón del jugador
- Se mueve **solo horizontalmente** (izquierda/derecha).
- Posición vertical fija en la parte inferior de la pantalla.
- Puede disparar **una bala a la vez** (mientras la bala anterior esté en pantalla, no puede disparar otra) — esto es regla clásica de Space Invaders.
- No puede salirse de los límites laterales de la pantalla.

### 1.2 Aliens / Extraterrestres

Tres tipos, cada uno con valor de puntos distinto:

| Tipo | Forma | Puntos | Posición típica (fila) |
|------|-------|--------|------------------------|
| **Squid** (calamar) | Pequeño, dos antenas | **10 pts** | Filas superiores |
| **Crab** (cangrejo) | Mediano, brazos extendidos | **20 pts** | Filas intermedias |
| **Octopus** (pulpo) | Grande, tentáculos | **40 pts** | Filas inferiores |

**Comportamiento de los aliens:**
- Se mueven **lateralmente como bloque** (todos a la vez en la misma dirección).
- Al llegar al borde de la pantalla, **bajan una fila** y cambian de dirección.
- Velocidad aumenta a medida que quedan menos aliens (clásico Space Invaders).
- Pueden disparar hacia abajo aleatoriamente.

**Configuración típica de oleada:**
- Matriz de aliens, por ejemplo: 5 filas × 11 columnas = 55 aliens.
- Fila 1 (arriba): Squids
- Filas 2-3: Crabs
- Filas 4-5 (abajo): Octopuses

> Las posiciones exactas las decide el "administrador" del servidor en tiempo real vía el mensaje `Crear (X, Y, Pts)`.

### 1.3 OVNI / Platillo volador

- Aparece **cada cierto tiempo** (recomendado: cada 25-30 segundos aleatorios) por encima de los aliens.
- Se mueve **en una sola dirección** durante su aparición:
  - Izquierda → Derecha, O
  - Derecha → Izquierda
  - La dirección se elige aleatoriamente al spawn.
- Otorga **puntos aleatorios** al ser derribado. El valor base es ingresado por el usuario administrador en el servidor, y se aplica un factor aleatorio (ej. valor base × random[0.5, 2.0]).
- Si cruza toda la pantalla sin ser derribado, desaparece sin dar puntos.

### 1.4 Bunkers

- **Exactamente 4 bunkers** distribuidos horizontalmente entre los aliens y el cañón.
- Cada bunker tiene 4 estados de daño: **100%, 70%, 40%, 0%**.
- Estados visuales: el bunker muestra progresivamente más destrucción.
- Se dañan cuando:
  - Una bala alien impacta el bunker (desde arriba).
  - Una bala del cañón impacta el bunker (desde abajo) — **sí, el cañón también daña sus propios bunkers**.
- Cuando un bunker llega a 0%, queda destruido y ya no bloquea balas.

### 1.5 Balas

Dos tipos:
- **Bala del cañón:** sube desde el cañón. Una a la vez por jugador.
- **Bala alien:** baja desde la fila inferior de aliens activos. Múltiples pueden coexistir.

Las balas:
- Se mueven en línea recta vertical.
- Desaparecen al impactar algo o salir de pantalla.

## 2. Sistema de puntuación

| Acción | Puntos |
|--------|--------|
| Derribar squid | **+10** |
| Derribar crab | **+20** |
| Derribar octopus | **+40** |
| Derribar OVNI | **Aleatorio** (basado en valor base del admin) |
| Limpiar oleada | **+1 vida** (no es puntos, es vida) |

El puntaje se mantiene **acumulativo entre oleadas** del mismo jugador.

## 3. Sistema de vidas

- El jugador inicia con **3 vidas**.
- Pierde **1 vida** cada vez que una bala alien lo impacta directamente.
- Gana **+1 vida** cada vez que elimina TODOS los aliens de la oleada actual.
- **No hay tope explícito** de vidas en el enunciado, pero se recomienda un máximo razonable (ej. 9) para evitar overflow visual.
- Cuando las vidas llegan a **0**, el juego termina (game over).

## 4. Oleadas (waves)

El ciclo de oleada es:

```
Inicio oleada → Aliens se mueven y disparan → Jugador dispara
   ↓                                              ↓
Aliens llegan al cañón (GAME OVER)    Todos los aliens eliminados
                                                  ↓
                              +1 vida al jugador
                                                  ↓
                              Aumenta velocidad de aliens
                                                  ↓
                              Nueva oleada (loop)
```

- El ciclo es **indefinido**: puede haber infinitas oleadas, cada vez más rápidas.
- Entre oleadas, los puntos se mantienen, las vidas se conservan (y +1).

## 5. Condiciones de Game Over

El juego termina cuando se cumple **cualquiera** de:

1. **Los invasores llegan al cañón** del jugador (cruzan la línea vertical donde está el cañón).
2. **Las vidas del jugador llegan a 0** por impactos de balas.

Al terminar:
- Mostrar puntaje final.
- Esperar input para reiniciar o salir.

## 6. Multijugador

El proyecto requiere **mínimo 2 jugadores simultáneos**. Cada jugador:
- Tiene su **propio puntaje**.
- Tiene sus **propias vidas**.
- Tiene su **propio cañón**.
- Comparte la **misma pantalla de aliens y bunkers**.

> **Decisión de diseño:** El enunciado no aclara si los jugadores comparten la misma matriz de aliens o tienen partidas paralelas. La interpretación recomendada (y más sencilla) es: **misma partida compartida** — ambos disparan a la misma oleada, los puntos van a quien derribe cada alien. Documentar esta decisión.

## 7. Detección de colisiones

Tipos de colisión a manejar:

| Origen | Destino | Efecto |
|--------|---------|--------|
| Bala cañón | Alien | Alien destruido, +puntos al jugador, bala destruida |
| Bala cañón | OVNI | OVNI destruido, +puntos aleatorios, bala destruida |
| Bala cañón | Bunker | Bunker pierde 1 nivel de salud, bala destruida |
| Bala alien | Cañón | Jugador pierde 1 vida, bala destruida |
| Bala alien | Bunker | Bunker pierde 1 nivel de salud, bala destruida |
| Bala cañón | Bala alien | Ambas se destruyen (opcional pero clásico) |
| Alien (cuerpo) | Cañón | Game over inmediato |

Algoritmo recomendado: **AABB collision** (Axis-Aligned Bounding Box) — comparar rectángulos. Es simple, rápido y suficiente para sprites 2D.

## 8. Velocidad de aliens

- **Velocidad inicial:** un valor base (ej. 1 movimiento cada 800ms).
- **Aumento por kill:** mientras menos aliens quedan, más rápido se mueven los restantes. Fórmula sugerida:
  ```
  intervalo = intervalo_base × (alien_count / total_inicial)
  ```
  Con un mínimo (ej. 50ms) para que no sea injugable.
- **Aumento por oleada:** la velocidad base se reduce un % cada nueva oleada (ej. -15% cada vez).
- **Control admin:** el mensaje `Velocidad N` permite al administrador modificar la velocidad en tiempo real.

## 9. Disparo de aliens

- Solo los **aliens de la fila inferior** de cada columna pueden disparar (los que están "al frente").
- Cada cierto tiempo aleatorio, un alien aleatorio dispara.
- Frecuencia base: ~1 disparo cada 1-2 segundos en total (no por alien).
- A medida que la velocidad aumenta, los disparos también pueden volverse más frecuentes.

## 10. Estados del juego (state machine)

El juego tiene estos estados a alto nivel:

```
MENU → PLAYING → PAUSED → PLAYING
                    ↓
                GAME_OVER → MENU
                    ↑
                NEW_WAVE (PLAYING continúa)
```

- **MENU:** pantalla inicial, espera conexión.
- **PLAYING:** juego en curso.
- **PAUSED:** opcional, pero útil.
- **NEW_WAVE:** transición breve mostrando "Wave N" antes de continuar a PLAYING.
- **GAME_OVER:** muestra puntaje final, espera input.

## 11. Mensajes del administrador

El "administrador" del servidor puede inyectar eventos al juego en tiempo real:

| Comando | Efecto |
|---------|--------|
| `Crear (X, Y, Pts)` | Spawnear alien en posición (X,Y) con valor Pts (Pts determina el tipo: 10→squid, 20→crab, 40→octopus). |
| `OVNI I-D 1500` | Spawnear OVNI moviéndose izquierda→derecha con valor base 1500. |
| `OVNI D-I 1500` | Spawnear OVNI moviéndose derecha→izquierda con valor base 1500. |
| `Velocidad 100` | Setear el intervalo de movimiento de aliens a 100ms. |
| `Bunkers 70%` | Setear todos los bunkers al estado 70% (o el bunker apuntado, según interpretación). |

> El admin puede ser una consola en el servidor (CLI) que el operador usa mientras los clientes juegan. Documentar bien en el manual de usuario.

## 12. Comportamientos opcionales (mejora)

Estos no son requeridos pero suman calidad:

- ✨ Animación de aliens (cambian de sprite mientras se mueven, clásico).
- ✨ Sonidos (disparo, explosión, OVNI).
- ✨ Pantalla de high scores.
- ✨ Modo pausa.
- ✨ Indicador visual de qué jugador eres (P1, P2).
- ✨ Explosión visual al destruir un alien.

## 13. Reglas que la gente suele olvidar

- 🔴 El cañón **SÍ** daña los bunkers al dispararles. No es solo el alien.
- 🔴 La velocidad aumenta dentro de la oleada (a menos aliens, más rápidos los restantes), Y entre oleadas.
- 🔴 +1 vida al limpiar oleada — no es "puntos extra", es vida real.
- 🔴 El OVNI da puntos ALEATORIOS, no un valor fijo.
- 🔴 Cada jugador tiene SU OWN espectador, no es un espectador global.
- 🔴 Game over si los aliens **TOCAN** el cañón, no solo si llegan al fondo.
