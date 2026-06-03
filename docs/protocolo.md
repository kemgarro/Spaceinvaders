# Protocolo de comunicación spaCEinvaders

Documento contrato entre los tres componentes de red del proyecto:

- **Servidor Java** (lógica del juego).
- **Cliente C jugador** (raylib, controla un cañón).
- **Cliente C espectador** (raylib, solo observa).

Este documento define el formato exacto de los mensajes que viajan sobre TCP entre el servidor y los clientes, así como una nota corta sobre el enlace UART entre el cliente C jugador y el controlador Raspberry Pi Pico.

---

## 1. Resumen y decisión

- **Transporte**: TCP, conexión persistente por cliente.
- **Codificación**: UTF-8.
- **Formato de mensaje**: JSON delimitado por línea (un objeto JSON completo por línea, terminado en `\n`). El servidor usa `BufferedReader.readLine()` y `PrintWriter.println(...)`; los clientes C deben leer hasta `\n` y parsear con cJSON.
- **Puerto por defecto**: `5555` (`Config.PUERTO_DEFAULT`).
- **Frecuencia de envío de estado**: hasta `TICKS_POR_SEGUNDO = 20` (un `STATE` por tick relevante o por notificación de cambio).
- **Tamaño de buffer recomendado**: `8192` bytes (`Config.BUFFER_SIZE`).

Un mensaje JSON nunca puede contener saltos de línea internos sin escapar — Gson lo garantiza al serializar, y el cliente C debe escapar `\n`, `"` y `\\` si llegara a generar JSON a mano.

---

## 2. Justificación técnica de JSON sobre TCP

Se eligió JSON line-delimited en lugar de un formato pipe-separated por las siguientes razones:

- **Extensibilidad sin romper el parser.** Agregar un campo nuevo a un mensaje (por ejemplo `color` a un alien, o `oleada` a un evento) no desplaza posiciones. Con pipe-separated por índices, cualquier campo nuevo obliga a actualizar ambos extremos en lock-step o el cliente lee basura.
- **Estructura jerárquica natural.** Un `STATE` contiene listas anidadas de aliens, balas, bunkers y jugadores. JSON modela esto directamente con arreglos y objetos. Con pipe-separated habría que inventar un sub-protocolo de bloques tipo `ALIEN|...|FIN_ALIENS|BALA|...|FIN_BALAS`, que es exactamente reescribir un parser de árbol con peor sintaxis.
- **Auto-descriptivo.** Cada valor está etiquetado por nombre. Esto permite inspeccionar tráfico con `nc`, `tcpdump` o un simple `cat` del log y entender qué pasó sin consultar una tabla de índices; también facilita detectar mensajes mal formados.
- **Tolerante al orden y a campos desconocidos.** El cliente puede ignorar campos que no entiende sin crashear, lo cual es útil mientras los tres equipos avanzan a velocidades distintas y un lado agrega campos antes que el otro los consuma.
- **Librerías estándar maduras en ambos lados.** Gson en Java y cJSON en C son dependencias únicas, ampliamente probadas; evitamos escribir y depurar un parser propio.
- **Costo aceptado.** JSON usa aproximadamente 3× más bytes que pipe-separated y su parsing es más caro en CPU. En una LAN, a 20 TPS y con un orden de magnitud de ~100 entidades activas, el costo es despreciable frente al beneficio de mantenibilidad.

> **Nota sobre documentos obsoletos.** Cualquier documento interno previo del equipo que describa el protocolo en formato pipe-separated (`STATE|...|...`) queda **desactualizado** a partir de este archivo y debe tratarse como referencia histórica. La fuente de verdad del protocolo es este documento.

---

## 3. Modelo de mensaje (DTO)

Todos los mensajes comparten la misma estructura raíz, definida en `Mensaje.java`:

| Campo        | Tipo               | Uso                                                                                       |
|--------------|--------------------|-------------------------------------------------------------------------------------------|
| `type`       | string (enum)      | `INPUT`, `STATE`, `EVENT`, `CONNECT`, `DISCONNECT`, `ERROR`, `ADMIN_CMD`                  |
| `id`         | string             | Identificador del cliente (jugador, espectador o admin). Obligatorio en `CONNECT`, `INPUT` y `ADMIN_CMD`. |
| `action`     | string             | Acción enviada por el jugador en mensajes `INPUT`.                                       |
| `name`       | string             | Nombre del evento en mensajes `EVENT` (corresponde a `TipoEvento`) o del comando en `ADMIN_CMD`. |
| `clientType` | string             | `PLAYER`, `SPECTATOR` o `ADMIN`. Solo se usa en `CONNECT`. Si falta, se asume `PLAYER`.   |
| `target`     | string             | Id del jugador observado. Obligatorio en `CONNECT` cuando `clientType = "SPECTATOR"`.    |
| `payload`    | objeto / valor     | Datos específicos del evento, del error o del comando admin.                              |
| `data`       | objeto             | Cuerpo del `STATE` (snapshot del juego).                                                 |

Los campos no usados por un tipo de mensaje pueden omitirse o llegar como `null`; el receptor debe tolerar ambos casos.

---

## 4. Sesión y handshake

Flujo de vida de una conexión:

1. El cliente abre un socket TCP contra `host:5555`.
2. El servidor acepta. Si no hay espacio (`gameManager.tieneEspacio()` retorna falso), envía un `ERROR` y cierra el socket.
3. El cliente envía un mensaje `CONNECT` con `id` y `clientType`.
4. El servidor valida:
   - Si `clientType == "PLAYER"`: intenta registrar al jugador. Si no cabe, responde `ERROR` y cierra.
   - Si `clientType == "SPECTATOR"`: verifica que haya al menos un jugador activo **y** que el espectador haya enviado el campo `target` con el id de un jugador existente. Si falta `target` o el jugador objetivo no existe, responde `ERROR` y cierra. No se aceptan espectadores "globales" — RF-CE03 del enunciado exige que cada espectador esté asociado a un jugador concreto.
   - Si `clientType == "ADMIN"`: registra al admin como observador sin consumir slots de jugador ni de espectador. El admin queda habilitado para enviar mensajes `ADMIN_CMD` por el mismo socket (ver sección 5.4).
5. Si la admisión fue exitosa, el servidor envía un `STATE` inicial al cliente.
6. A partir de ese momento:
   - El servidor empuja `STATE` (snapshots periódicos) y `EVENT` (cambios puntuales) al cliente.
   - El jugador envía `INPUT` cuando hay actividad del usuario o del controlador físico.
   - El espectador no envía `INPUT` — si lo hace, el servidor responde con `ERROR` y descarta el mensaje.
7. El cliente puede cerrar con un `DISCONNECT` explícito o simplemente cerrando el socket; ambos casos llevan al servidor a limpiar al observer y liberar el slot.

---

## 5. Mensajes cliente → servidor

### 5.1 `CONNECT`

Primer mensaje obligatorio tras abrir el socket. Identifica al cliente y declara su tipo.

```json
{
  "type": "CONNECT",
  "id": "jugador-1",
  "clientType": "PLAYER"
}
```

Espectador (debe declarar a qué jugador va a observar mediante `target`):

```json
{
  "type": "CONNECT",
  "id": "espectador-1",
  "clientType": "SPECTATOR",
  "target": "jugador-1"
}
```

Reglas:

- Si `id` es nulo o vacío, el servidor responde `ERROR` y desconecta.
- Si `clientType` falta, se asume `"PLAYER"` por compatibilidad (`ManejadorCliente.manejarConexion`).
- Valores aceptados de `clientType`: `"PLAYER"`, `"SPECTATOR"` (comparación case-insensitive).
- Si `clientType == "SPECTATOR"`, el campo `target` es **obligatorio** y debe contener el id de un jugador actualmente registrado:
  - Si `target` falta o está vacío, el servidor responde `ERROR` con el texto `"Espectador requiere campo target (id del jugador a observar)"` y cierra el socket.
  - Si `target` apunta a un id que no existe (o se rechaza por cupo lleno), responde `ERROR` con `"No se puede conectar como espectador: jugador target inexistente o cupo lleno: <target>"` y cierra.
  - El servidor trackea la relación espectador → jugador en `EstadoJuego.espectadoresPorJugador`, expuesta en el snapshot `STATE` bajo la clave `espectadoresPorJugador` para diagnóstico.

### 5.2 `INPUT`

Solo lo envían jugadores. Codifica una acción discreta del cañón.

```json
{
  "type": "INPUT",
  "id": "jugador-1",
  "action": "MOVE_LEFT"
}
```

**Convención de acciones acordada** (literales que el servidor reconoce en `GameManager.procesarInput`):

| `action`     | Significado                                  |
|--------------|----------------------------------------------|
| `MOVE_LEFT`  | Mover cañón un paso a la izquierda           |
| `MOVE_RIGHT` | Mover cañón un paso a la derecha             |
| `FIRE`       | Disparar (respetando `MAX_BALAS_JUGADOR = 1`)|
| `STOP`       | Detener movimiento (opcional, ver nota)      |

> **Ambigüedad pendiente para el equipo.** El servidor reutilizable aún no fija si el cañón se mueve por "pulsos" (un `MOVE_LEFT` mueve un paso fijo) o por "estado mantenido" (mover mientras no llegue `STOP`). Mientras se decide, el cliente C jugador enviará un `MOVE_LEFT`/`MOVE_RIGHT` por tecla presionada y un `STOP` al soltar; el servidor puede ignorar `STOP` si el modelo termina siendo por pulsos. Este documento debe actualizarse al cerrar la decisión.

Los espectadores que envíen `INPUT` reciben este `ERROR` y el mensaje se descarta:

```json
{
  "type": "ERROR",
  "payload": { "error": "Los espectadores no pueden enviar inputs" }
}
```

### 5.3 `DISCONNECT`

Cierre cortés. El servidor libera el slot y deja de notificar al observer.

```json
{
  "type": "DISCONNECT",
  "id": "jugador-1"
}
```

Si el socket se cae sin `DISCONNECT`, el servidor lo trata de la misma forma al detectar `IOException` en la lectura.

### 5.4 `ADMIN_CMD`

Lo envía únicamente un cliente que se identificó con `clientType: "ADMIN"` en el `CONNECT`. Si lo envía un jugador o un espectador, el servidor responde `ERROR` y descarta el mensaje. El campo `name` indica el comando del enunciado y `payload` lleva los parámetros.

| `name`           | Payload                                              | Efecto                                                              |
|------------------|------------------------------------------------------|---------------------------------------------------------------------|
| `CREATE_ALIEN`   | `{ "x": <num>, "y": <num>, "pts": 10\|20\|40 }`     | Spawnea un alien del tipo indicado (10=Squid, 20=Crab, 40=Octopus). |
| `SPAWN_OVNI`     | `{ "direccion": "I-D"\|"D-I", "puntosBase": <num> }`| Lanza un OVNI con la dirección y el valor base solicitados.         |
| `SET_VELOCIDAD`  | `{ "intervaloMs": <num> }`                          | Ajusta el intervalo BASE del bloque de aliens (clamp a `[MIN, MAX]`).|
| `SET_BUNKERS`    | `{ "pct": 0..100 }`                                 | Fija la salud de los 4 bunkers al porcentaje indicado.              |

Ejemplos:

```json
{ "type": "ADMIN_CMD", "id": "admin", "name": "CREATE_ALIEN",
  "payload": { "x": 400, "y": 200, "pts": 40 } }
```

```json
{ "type": "ADMIN_CMD", "id": "admin", "name": "SPAWN_OVNI",
  "payload": { "direccion": "I-D", "puntosBase": 1500 } }
```

```json
{ "type": "ADMIN_CMD", "id": "admin", "name": "SET_VELOCIDAD",
  "payload": { "intervaloMs": 150 } }
```

```json
{ "type": "ADMIN_CMD", "id": "admin", "name": "SET_BUNKERS",
  "payload": { "pct": 70 } }
```

El servidor loguea cada comando ejecutado y, si el comando es inválido (parámetros faltantes, tipo de alien no soportado, OVNI ya activo, etc.), responde con un `ERROR` describiendo el motivo, sin tumbar el game loop.

---

## 6. Mensajes servidor → cliente

### 6.1 `STATE`

Snapshot completo del estado del juego. Es el mensaje principal que dibuja el cliente. Va dentro del campo `data`.

```json
{
  "type": "STATE",
  "data": {
    "oleada": 2,
    "campo": { "ancho": 800, "alto": 600 },
    "jugadores": [
      {
        "id": "jugador-1",
        "x": 380.0,
        "y": 550,
        "vidas": 3,
        "puntaje": 240,
        "vivo": true
      }
    ],
    "aliens": [
      { "id": "a-0",  "tipo": "SQUID",   "x": 100, "y": 80,  "vivo": true },
      { "id": "a-1",  "tipo": "CRAB",    "x": 140, "y": 80,  "vivo": true },
      { "id": "a-2",  "tipo": "OCTOPUS", "x": 180, "y": 80,  "vivo": true }
    ],
    "balas": [
      { "id": "b-12", "origen": "JUGADOR", "x": 392, "y": 510, "vx": 0.0, "vy": -8.0 },
      { "id": "b-13", "origen": "ALIEN",   "x": 220, "y": 200, "vx": 0.0, "vy":  4.0 }
    ],
    "bunkers": [
      { "id": "bunker-0", "x": 120, "y": 470, "salud": 100 },
      { "id": "bunker-1", "x": 300, "y": 470, "salud": 70  },
      { "id": "bunker-2", "x": 480, "y": 470, "salud": 40  },
      { "id": "bunker-3", "x": 660, "y": 470, "salud": 0   }
    ],
    "ovni": {
      "activo": true,
      "x": 50,
      "y": 40,
      "direccion": "I-D",
      "puntosBase": 100
    },
    "velocidadAliensMs": 680,
    "tiempoServidorMs": 1738291234567
  }
}
```

Reglas para el cliente:

- Si un campo no aparece, asumirlo en su valor por defecto razonable (lista vacía, `false`, `0`).
- Si aparece un campo desconocido, ignorarlo.
- El `STATE` reemplaza al estado anterior; no es un delta.

### 6.2 `EVENT`

Notificaciones puntuales de cambios. Permiten al cliente reaccionar con sonido o efectos sin esperar al próximo `STATE`. El campo `name` siempre corresponde a un valor del enum `TipoEvento`.

Eventos definidos (`EventoJuego.TipoEvento`):

| `name`               | Cuándo se emite                                        |
|----------------------|--------------------------------------------------------|
| `ALIEN_DESTROYED`    | Un alien fue destruido                                 |
| `UFO_DESTROYED`      | El OVNI fue destruido                                  |
| `PLAYER_HIT`         | El jugador fue golpeado por una bala alien             |
| `PLAYER_LIFE_LOST`   | El jugador perdió una vida                             |
| `PLAYER_LIFE_GAINED` | El jugador ganó una vida (típicamente al limpiar oleada) |
| `PLAYER_ELIMINATED`  | El jugador llegó a 0 vidas                             |
| `WAVE_CLEARED`       | Oleada limpiada                                        |
| `WAVE_STARTED`       | Nueva oleada                                           |
| `BUNKER_DAMAGED`     | Un bunker recibió daño                                 |
| `BUNKER_DESTROYED`   | Un bunker quedó destruido (salud = 0)                  |
| `GAME_OVER`          | Fin del juego                                          |
| `SPEED_CHANGED`      | Cambió la velocidad de los aliens                      |

Ejemplos:

```json
{
  "type": "EVENT",
  "name": "ALIEN_DESTROYED",
  "payload": {
    "alienId": "a-12",
    "tipo": "CRAB",
    "puntos": 20,
    "killerId": "jugador-1"
  }
}
```

```json
{
  "type": "EVENT",
  "name": "UFO_DESTROYED",
  "payload": {
    "puntos": 150,
    "killerId": "jugador-1"
  }
}
```

```json
{
  "type": "EVENT",
  "name": "PLAYER_HIT",
  "payload": { "id": "jugador-1" }
}
```

```json
{
  "type": "EVENT",
  "name": "PLAYER_LIFE_LOST",
  "payload": { "id": "jugador-1", "vidasRestantes": 2 }
}
```

```json
{
  "type": "EVENT",
  "name": "PLAYER_LIFE_GAINED",
  "payload": { "id": "jugador-1", "vidasRestantes": 3 }
}
```

```json
{
  "type": "EVENT",
  "name": "PLAYER_ELIMINATED",
  "payload": { "id": "jugador-1" }
}
```

```json
{
  "type": "EVENT",
  "name": "WAVE_CLEARED",
  "payload": { "oleada": 2 }
}
```

```json
{
  "type": "EVENT",
  "name": "WAVE_STARTED",
  "payload": { "oleada": 3, "velocidadAliensMs": 580 }
}
```

```json
{
  "type": "EVENT",
  "name": "BUNKER_DAMAGED",
  "payload": { "bunkerId": "bunker-1", "salud": 70 }
}
```

```json
{
  "type": "EVENT",
  "name": "BUNKER_DESTROYED",
  "payload": { "bunkerId": "bunker-3" }
}
```

```json
{
  "type": "EVENT",
  "name": "GAME_OVER",
  "payload": { "ganador": null, "puntajeFinal": 1240 }
}
```

```json
{
  "type": "EVENT",
  "name": "SPEED_CHANGED",
  "payload": { "intervaloMs": 480 }
}
```

> **Ambigüedad pendiente.** Los nombres de campos dentro de `payload` (por ejemplo `alienId`, `killerId`, `vidasRestantes`) no están fijados por el código reutilizable: el `payload` es `Object` opaco. Esta tabla los propone como convención del equipo; al implementar, el servidor debe respetar estos nombres para que el cliente no tenga que adivinar.

### 6.3 `ERROR`

El servidor lo envía cuando un mensaje no se puede procesar o cuando rechaza una conexión. Es informativo; el cliente puede mostrarlo en consola y, según el caso, cerrar.

```json
{
  "type": "ERROR",
  "payload": { "error": "Servidor lleno: máximo 1 jugador + 1 espectador" }
}
```

> El método de rechazo temprano en `ServidorJuego.enviarMensajeRechazo` actualmente serializa el mensaje a mano y usa la clave `message` en lugar de `payload.error`. El equipo del servidor debe unificarlo con `JsonUtil.crearMensajeError` para que el cliente solo tenga que parsear un formato.

---

## 7. Comandos del administrador

El administrador puede inyectar los comandos del enunciado por dos canales equivalentes. Ambos terminan llamando los mismos métodos del motor (`crearAlienAdmin`, `crearOvniAdmin`, `setVelocidadAliens`, `setSaludBunkers`).

### 7.1 Admin remoto vía socket (recomendado)

Es la forma alineada con la arquitectura distribuida del proyecto: el admin es un **cliente más**, se conecta al mismo puerto TCP que los jugadores con `clientType: "ADMIN"` y manda sus órdenes como mensajes `ADMIN_CMD` (sección 5.4). El proyecto incluye un cliente Swing en el paquete `cr.ac.tec.spaceinvaders.admin` que provee una GUI con secciones para cada comando.

| Comando enunciado        | `ADMIN_CMD.name` | Payload                                              |
|--------------------------|------------------|------------------------------------------------------|
| `Crear (X, Y, Pts)`      | `CREATE_ALIEN`   | `{ "x": X, "y": Y, "pts": Pts }`                     |
| `OVNI I-D <pts>`         | `SPAWN_OVNI`     | `{ "direccion": "I-D", "puntosBase": pts }`          |
| `OVNI D-I <pts>`         | `SPAWN_OVNI`     | `{ "direccion": "D-I", "puntosBase": pts }`          |
| `Velocidad <ms>`         | `SET_VELOCIDAD`  | `{ "intervaloMs": ms }`                              |
| `Bunkers <pct>%`         | `SET_BUNKERS`    | `{ "pct": pct }`                                     |

Lanzar la GUI:

```bash
cd server-java
java -cp build/libs/spaceinvaders-server-1.0.0.jar \
     cr.ac.tec.spaceinvaders.admin.AdminApp \
     --host 127.0.0.1 --port 5555 --id admin
```

### 7.2 Consola CLI local (fallback)

La terminal donde corre el `java -jar` del servidor expone los mismos comandos en su sintaxis literal del enunciado. Esta vía no viaja por TCP — son texto plano leído de stdin — pero su efecto se refleja en el siguiente `STATE` y, donde aplica, en un `EVENT`. Se conserva para uso offline o como respaldo si el cliente Swing no está disponible.

| Comando admin            | Sintaxis exacta            | Efecto                                                                 |
|--------------------------|----------------------------|------------------------------------------------------------------------|
| Crear alien              | `Crear (X, Y, Pts)`        | Spawnea un alien del tipo indicado por `Pts` (10/20/40).               |
| OVNI izquierda a derecha | `OVNI I-D <pts>`           | Lanza un OVNI moviéndose hacia la derecha con valor base `<pts>`.      |
| OVNI derecha a izquierda | `OVNI D-I <pts>`           | Lanza un OVNI moviéndose hacia la izquierda.                           |
| Cambiar velocidad aliens | `Velocidad <ms>`           | Ajusta el intervalo BASE del bloque de aliens.                         |
| Reparar bunkers          | `Bunkers <pct>%`           | Fija la salud de los 4 bunkers al porcentaje indicado.                 |

Los espacios y la capitalización del enunciado se respetan tal cual. El servidor debe loguear cada comando y rechazar (con log + mensaje en consola admin) los mal formados, sin tumbar el juego.

---

## 8. Protocolo UART Pico ↔ cliente C jugador

Fuera del alcance de TCP pero documentado aquí para que el equipo del cliente C sepa qué traducir:

- **Enlace**: UART, **115200 baud, 8N1**, sin control de flujo.
- **Formato**: 1 byte ASCII por evento físico, sin terminador.

| Byte | Evento físico                              | Acción enviada al servidor |
|------|--------------------------------------------|----------------------------|
| `L`  | 1 toque corto del botón 1                  | `INPUT` con `action: "MOVE_LEFT"`  |
| `R`  | 2 toques cortos del botón 1                | `INPUT` con `action: "MOVE_RIGHT"` |
| `F`  | Botón 2                                    | `INPUT` con `action: "FIRE"`       |

El cliente C jugador es el único que abre el puerto serial; el espectador no toca UART. El cliente C traduce cada byte a un mensaje JSON `INPUT` antes de mandarlo por el socket TCP al servidor.

---

## 9. Manejo de errores y desconexiones

- **Mensaje JSON mal formado.** El servidor logea el error y descarta el mensaje. Opcionalmente puede responder con un `ERROR` describiendo la causa (`ManejadorCliente.procesarMensaje` ya lo hace).
- **Tipo de mensaje desconocido.** Se ignora con un log de nivel debug.
- **Cliente sin `id` en `CONNECT`.** El servidor responde `ERROR` y cierra el socket.
- **Servidor lleno al aceptar conexión.** El servidor responde un `ERROR` y cierra (ver nota en sección 6.3 sobre unificar el formato).
- **Cliente desconecta abruptamente.** El servidor recibe `IOException` o `readLine()` retorna `null`; ejecuta el mismo flujo que para `DISCONNECT` (elimina jugador o espectador del `GameManager`, libera el slot y se desregistra como observer).
- **Espectador intenta `INPUT`.** Se responde `ERROR` y el mensaje se descarta sin afectar la simulación.
- **Excepción no anticipada al procesar.** Se logea como error y se notifica con `ERROR` al cliente, sin tumbar el hilo del manejador.

Cada cliente corre en su propio hilo del `ExecutorService` del servidor, por lo que la caída de uno no afecta a los demás.

---

## 10. Sesión de ejemplo completa

Líneas marcadas como `→` van del cliente al servidor; `←` van del servidor al cliente. Cada bloque representa una sola línea JSON terminada en `\n`.

**1. Apertura y handshake.**

```json
→ {"type":"CONNECT","id":"jugador-1","clientType":"PLAYER"}
```

```json
← {"type":"STATE","data":{"oleada":1,"campo":{"ancho":800,"alto":600},"jugadores":[{"id":"jugador-1","x":400.0,"y":550,"vidas":3,"puntaje":0,"vivo":true}],"aliens":[{"id":"a-0","tipo":"SQUID","x":100,"y":80,"vivo":true}],"balas":[],"bunkers":[{"id":"bunker-0","x":120,"y":470,"salud":100}],"ovni":{"activo":false},"velocidadAliensMs":800,"tiempoServidorMs":1738291230000}}
```

**2. El jugador dispara.**

```json
→ {"type":"INPUT","id":"jugador-1","action":"FIRE"}
```

**3. El servidor empuja el siguiente snapshot con la bala en vuelo.**

```json
← {"type":"STATE","data":{"oleada":1,"jugadores":[{"id":"jugador-1","x":400.0,"y":550,"vidas":3,"puntaje":0,"vivo":true}],"aliens":[{"id":"a-0","tipo":"SQUID","x":100,"y":80,"vivo":true}],"balas":[{"id":"b-1","origen":"JUGADOR","x":408,"y":540,"vx":0.0,"vy":-8.0}],"bunkers":[{"id":"bunker-0","x":120,"y":470,"salud":100}],"ovni":{"activo":false},"velocidadAliensMs":800,"tiempoServidorMs":1738291230050}}
```

**4. Impacto: el servidor emite un evento y el siguiente STATE refleja la baja.**

```json
← {"type":"EVENT","name":"ALIEN_DESTROYED","payload":{"alienId":"a-0","tipo":"SQUID","puntos":10,"killerId":"jugador-1"}}
```

```json
← {"type":"STATE","data":{"oleada":1,"jugadores":[{"id":"jugador-1","x":400.0,"y":550,"vidas":3,"puntaje":10,"vivo":true}],"aliens":[],"balas":[],"bunkers":[{"id":"bunker-0","x":120,"y":470,"salud":100}],"ovni":{"activo":false},"velocidadAliensMs":800,"tiempoServidorMs":1738291230400}}
```

**5. Cierre.**

```json
→ {"type":"DISCONNECT","id":"jugador-1"}
```

El servidor cierra el socket, elimina al jugador del `GameManager` y libera el slot.

---

## 11. Tabla resumen

| Tipo de mensaje | Dirección       | Campos relevantes                            | Notas                                              |
|-----------------|------------------|----------------------------------------------|----------------------------------------------------|
| `CONNECT`       | cliente → servidor | `id`, `clientType`, `target` (solo espectador) | Primer mensaje obligatorio. El espectador debe declarar a quién observa con `target`. |
| `INPUT`         | cliente → servidor | `id`, `action`                              | Solo jugadores. Acciones: `MOVE_LEFT`, `MOVE_RIGHT`, `FIRE`, `STOP`. |
| `DISCONNECT`    | cliente → servidor | `id`                                        | Cortés. Cerrar el socket equivale a este mensaje.  |
| `STATE`         | servidor → cliente | `data` (snapshot completo)                  | Reemplaza estado, no es delta.                     |
| `EVENT`         | servidor → cliente | `name` (de `TipoEvento`), `payload`         | Notificación puntual.                              |
| `ERROR`         | servidor → cliente | `payload.error`                             | Informativo; puede preceder a un cierre de socket. |

Parámetros base del transporte:

| Parámetro                  | Valor                              |
|----------------------------|------------------------------------|
| Puerto TCP                 | `5555`                             |
| Codificación               | UTF-8                              |
| Delimitador                | `\n` (un JSON por línea)           |
| Ticks por segundo          | 20                                 |
| Máx. jugadores             | 4                                  |
| Máx. espectadores/jugador  | 2                                  |
| Buffer recomendado         | 8192 bytes                         |

Cualquier cambio al protocolo debe pasar por una actualización de este documento antes de mergearse al servidor o a los clientes.
