---
name: requirements
description: Lista exhaustiva de requerimientos funcionales y no funcionales del proyecto spaCEinvaders extraídos del enunciado oficial. Consultar SIEMPRE antes de implementar cualquier funcionalidad para verificar que cumple con lo requerido. Usar especialmente cuando se vaya a implementar lógica del juego, mensajes de red, interfaz, control físico, o cuando haya dudas sobre qué se espera entregar.
---

# Requerimientos del sistema spaCEinvaders

Este documento contiene **todos** los requerimientos extraídos del enunciado oficial. Antes de implementar cualquier funcionalidad, verificar contra esta lista.

## 1. Requerimientos funcionales

### 1.1 Servidor (Java)

- **RF-S01:** Mantiene TODA la lógica del juego (no se permite lógica en el cliente).
- **RF-S02:** Crea y destruye extraterrestres en posiciones que el administrador decida en cualquier momento de la partida.
- **RF-S03:** Lleva la puntuación de cada jugador.
- **RF-S04:** Lleva las vidas restantes de cada jugador. Inicia con **3 vidas**. Cada impacto de bala enemiga reduce 1 vida.
- **RF-S05:** Crea OVNIs con valor aleatorio (introducido por el usuario administrador).
- **RF-S06:** Controla la velocidad de desplazamiento de los extraterrestres.
- **RF-S07:** Controla el estado de los bunkers (porcentaje de daño).
- **RF-S08:** Todas las variables anteriores se pueden modificar en tiempo de ejecución vía mensajes.
- **RF-S09:** Cuando el jugador elimina TODOS los extraterrestres:
  - Se asigna **+1 vida** al jugador.
  - El juego reinicia con velocidad de extraterrestres **mayor** a la anterior.
  - Este ciclo es **indefinido**.
- **RF-S10:** Si los invasores llegan al cañón del jugador, el juego termina (game over).
- **RF-S11:** Si las vidas llegan a 0, el juego termina.
- **RF-S12:** Soporta **mínimo 2 clientes jugadores** simultáneos para revisión.

### 1.2 Mensajes que el servidor debe manejar (sección 4.1.1 del enunciado)

- **RF-M01:** `Crear Extraterrestre (X, Y, Pts)` — ejemplo: `Crear (1,1,1000)`
- **RF-M02:** `Crear OVNI dirección puntos` — ejemplo: `OVNI I-D 1500` (I-D = izquierda a derecha)
- **RF-M03:** `Aumentar Velocidad de desplazamiento` — ejemplo: `Velocidad 100`
- **RF-M04:** `Estado de Bunkers` — ejemplos: `Bunkers 70%`, `Bunkers 40%`, `Bunkers 0%`

> Ver `network-protocol/SKILL.md` para la especificación completa del protocolo.

### 1.3 Tipos de extraterrestres y puntuaciones

| Tipo | Forma | Puntos al ser derribado |
|------|-------|------------------------|
| Calamar | squid | **10 pts** |
| Cangrejo | crab | **20 pts** |
| Pulpo | octopus | **40 pts** |
| OVNI / Platillo | UFO | **Aleatorio** (valor base ingresado por usuario) |

### 1.4 OVNI / Platillo volador

- **RF-O01:** Aparece "cada cierto tiempo" por encima de los invasores.
- **RF-O02:** Se mueve aleatoriamente: izquierda→derecha o derecha→izquierda.
- **RF-O03:** Otorga puntos extras en cantidad **aleatoria** al ser derribado.

### 1.5 Bunkers (escudos de protección)

- **RF-B01:** Hay **exactamente 4 bunkers** de protección terrestre.
- **RF-B02:** Cubren al jugador del fuego alienígena.
- **RF-B03:** Se destruyen **gradualmente** por:
  - Disparos de los invasores (desde arriba).
  - Disparos del cañón del jugador (desde abajo, también dañan al bunker).
- **RF-B04:** Estados de daño visibles: 100%, 70%, 40%, 0%.

### 1.6 Cliente Jugador (C)

- **RF-CJ01:** Es la interfaz gráfica del juego.
- **RF-CJ02:** Controla el cañón que utiliza el usuario (movimiento izquierda/derecha + disparo).
- **RF-CJ03:** Interpreta la estructura/mensajes enviados por el servidor y renderiza el estado.
- **RF-CJ04:** Informa al servidor cuando un extraterrestre es eliminado (vía mensaje).
- **RF-CJ05:** No contiene lógica de juego, solo presentación e input.

### 1.7 Cliente Espectador

- **RF-CE01:** Se puede unir a una partida existente.
- **RF-CE02:** Solo observa, no envía eventos de juego.
- **RF-CE03:** Cada jugador tiene su propio espectador asociado (ver diagrama del enunciado: "Ver Jugador 1", "Ver Jugador 2").

### 1.8 Control físico (Raspberry Pi Pico)

- **RF-CP01:** Implementado con **Raspberry Pi Pico** programado en **C**.
- **RF-CP02:** Tiene **2 botones físicos**.
- **RF-CP03:** Botón 1 controla el cañón:
  - **1 toque** → mover a la **izquierda**.
  - **2 toques** → mover a la **derecha**.
- **RF-CP04:** Botón 2 es el botón de **disparo**.
- **RF-CP05:** Comunicación con el cliente vía **UART o I2C**.

### 1.9 Comunicación

- **RF-N01:** Servidor ↔ Clientes vía **Sockets**.
- **RF-N02:** Pico ↔ Cliente C vía **UART o I2C**.

## 2. Requerimientos no funcionales

### 2.1 Lenguajes y paradigmas

- **RNF-01:** Servidor en **Java** usando paradigma **orientado a objetos** (clases, paquetes, herencia, polimorfismo).
- **RNF-02:** Cliente en **C** usando paradigma **imperativo**.
- **RNF-03:** Pico en **C**.
- **RNF-04:** Violar el paradigma asignado a un lenguaje = **nota 0** (regla 10.6).

### 2.2 Java: Patrones de diseño

- **RNF-05:** Implementar **mínimo 2 patrones de diseño** en el servidor Java.
- **RNF-06:** Patrones sugeridos: **Adapter, Observador (Observer), Abstract Factory, Factory**.
- **RNF-07:** **Singleton NO cuenta** como patrón para la revisión.

### 2.3 C: Buenas prácticas

- **RNF-08:** Todas las constantes deben estar en un **archivo aparte** (ej. `constants.h`).
- **RNF-09:** Uso obligatorio de **structs** para representar entidades.
- **RNF-10:** Se evaluará calidad del código imperativo.

### 2.4 Estructuras de datos

- **RNF-11:** Crear y manipular **listas** como estructuras de datos (objetivo específico de la tarea).
- **RNF-12:** Documentar la estructura del nodo de la lista.

### 2.5 Entregables

- **RNF-13:** Código fuente comentado.
- **RNF-14:** **Ejecutables** para el servidor, el cliente C y el firmware del Pico (no solo código fuente).
- **RNF-15:** Manual de usuario.
- **RNF-16:** Documento de diseño (ver sección 6 del enunciado, 9 puntos).
- **RNF-17:** **Bitácora digital** describiendo todas las actividades del equipo.
- **RNF-18:** Anexo de Atributos DI (Diseño) y AC (Aprendizaje Continuo) — vale 5% adicional.

### 2.6 Calidad y robustez

- **RNF-19:** Cada **excepción no manejada** durante la ejecución cuesta **2 puntos** de la nota final.
- **RNF-20:** El código **debe compilar** o nota = 0.
- **RNF-21:** Solo se revisan funcionalidades **completas e integradas**, no parciales.

## 3. Lista de verificación previa a la entrega

Antes de considerar el proyecto terminado, verificar:

- [ ] El servidor Java compila sin errores ni warnings críticos.
- [ ] El cliente C compila sin errores ni warnings críticos.
- [ ] El firmware del Pico compila y se puede flashear.
- [ ] Se generan ejecutables para los 3 componentes.
- [ ] El servidor acepta al menos 2 clientes jugadores simultáneos.
- [ ] Los 3 tipos de aliens otorgan los puntos correctos (10/20/40).
- [ ] El OVNI aparece, se mueve aleatoriamente y otorga puntos aleatorios.
- [ ] Hay 4 bunkers que se degradan en estados 100/70/40/0%.
- [ ] El jugador inicia con 3 vidas y pierde 1 por impacto.
- [ ] Al eliminar todos los aliens se gana 1 vida y aumenta la velocidad.
- [ ] Game over si los aliens llegan al cañón o vidas = 0.
- [ ] El control Pico funciona: 1 toque = izq, 2 toques = der, botón 2 = disparo.
- [ ] Comunicación Pico↔Cliente por UART/I2C funciona.
- [ ] El cliente espectador puede unirse y observar.
- [ ] Se manejan desconexiones de cliente sin crashear el servidor.
- [ ] Constantes del cliente C están en archivo separado.
- [ ] Hay al menos 2 patrones de diseño identificables en el servidor Java.
- [ ] Listas están implementadas y documentadas.
- [ ] Documento de diseño con los 9 puntos requeridos.
- [ ] Bitácora completa y al día.
- [ ] Anexo de Atributos con secciones DI y AC.
