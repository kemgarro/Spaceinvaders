---
name: constraints
description: Restricciones absolutas, prohibiciones y reglas que NUNCA deben violarse al escribir código para el proyecto spaCEinvaders. Consultar SIEMPRE antes de generar código nuevo o tomar decisiones de diseño. Incluye paradigmas obligatorios, librerías prohibidas, anti-patrones a evitar, y reglas del enunciado que invalidan automáticamente el proyecto (nota 0) si se violan. Si tienes duda sobre si algo está permitido, consulta este documento primero.
---

# Restricciones del proyecto spaCEinvaders

Este documento define **qué NO se puede hacer**. Violar cualquiera de estas reglas puede resultar en nota 0 o pérdida significativa de puntos.

## 1. Restricciones que dan NOTA 0 (críticas)

Estas son del enunciado oficial (sección 10):

### 1.1 De paradigma
- ❌ **NO usar OOP en C.** El cliente C debe ser estrictamente **imperativo**. Nada de simular clases con structs+function pointers de forma exagerada. Funciones libres + structs simples.
- ❌ **NO usar programación imperativa en Java.** Todo debe estar encapsulado en clases. Nada de métodos `main` de 500 líneas haciendo todo. Nada de clases que sean solo contenedores de métodos estáticos.
- ❌ **NO mezclar lógica de negocio entre cliente y servidor.** TODA la lógica vive en el servidor Java.

### 1.2 De entrega
- ❌ **NO entregar después de la fecha** (11 de junio de 2026). Entrega tardía = 0.
- ❌ **NO entregar sin documentación.** Sin documentación = 0.
- ❌ **NO entregar sin el punto 3 de documentación** (descripción detallada de algoritmos). Sin esto = 0.
- ❌ **NO entregar código que no compile.** No compila = 0.

### 1.3 De integridad
- ❌ **NO copiar código de otros grupos** ni de internet sin atribuir. Indicio de copia = 0 y proceso disciplinario.

## 2. Restricciones de patrones de diseño (Java)

- ❌ **Singleton NO cuenta** como uno de los 2 patrones requeridos. Si se usa Singleton, debe ser ADICIONAL a los otros 2.
- ❌ **NO usar patrones "decorativos"** que no aportan al diseño. El profesor preguntará por qué se usó cada patrón.
- ❌ **NO confundir Factory Method con Abstract Factory** en la documentación. Si se llaman Abstract Factory, debe crear familias de productos relacionados.
- ❌ **NO implementar Observer manualmente cuando hay alternativas más limpias** si no se va a usar correctamente. Mejor hacerlo bien que hacerlo a medias.

## 3. Restricciones del cliente C

### 3.1 De estructura
- ❌ **NO poner constantes mágicas dispersas** en el código (números, strings, paths). TODAS las constantes van en archivo aparte (ej. `constants.h`).
- ❌ **NO declarar structs sin razón.** Usar structs cuando tenga sentido modelar una entidad (Alien, Bullet, Bunker, etc.), no envolver primitivos sin propósito.
- ❌ **NO usar variables globales innecesarias.** Si una función necesita estado, pásalo como parámetro o usa un struct contextual.

### 3.2 De librerías y dependencias
- ⚠️ **NO usar librerías C++ ni `iostream`** — esto es C puro, no C++.
- ⚠️ Preferir librerías estándar y bien establecidas para gráficos (raylib o SDL2). Evitar librerías oscuras o sin mantenimiento.
- ❌ **NO depender de librerías que no se puedan instalar fácilmente** en la máquina del profesor durante la revisión.

### 3.3 De estilo
- ❌ **NO usar `goto`** salvo casos legítimos de cleanup (raro en este proyecto).
- ❌ **NO ignorar valores de retorno** de funciones que pueden fallar (`malloc`, `socket`, `read`, etc.).
- ❌ **NO hacer fugas de memoria.** Todo `malloc` debe tener su `free`.

## 4. Restricciones del servidor Java

- ❌ **NO usar `System.out.println` para logging serio.** Usar `java.util.logging` o similar, o al menos centralizar el logging.
- ❌ **NO atrapar excepciones con `catch(Exception e) {}` vacío.** Cada excepción debe manejarse o relanzarse con contexto.
- ❌ **NO usar tipos primitivos cuando se necesita un objeto** (ej. usar `Integer` en listas, no intentar usar `int` directamente).
- ❌ **NO compartir estado mutable entre threads sin sincronización.** El servidor maneja múltiples clientes concurrentes.
- ❌ **NO crear clases "Dios"** (god classes) que hagan demasiado. Separar responsabilidades.

## 5. Restricciones del Pico

- ❌ **NO usar `delay()` largos** que bloqueen la detección de botones. Usar timers o lectura no bloqueante.
- ❌ **NO ignorar el debounce de botones.** Los botones físicos rebotan y producen lecturas falsas si no se filtran.
- ❌ **NO usar el Pico W (con WiFi) si no es necesario.** El proyecto pide UART/I2C, no WiFi. Mantenerse simple.

## 6. Restricciones de comunicación

- ❌ **NO usar protocolos propietarios complejos** (RMI, gRPC, etc.) cuando el enunciado dice "Sockets". Usar Sockets TCP estándar.
- ❌ **NO asumir orden de llegada de mensajes UDP** — usar TCP si el orden importa (que sí importa aquí).
- ❌ **NO enviar mensajes binarios opacos** sin documentar el protocolo. Preferir texto legible (líneas terminadas con `\n`, separadas por `|`, o JSON simple).

## 7. Restricciones de funcionalidad

- ❌ **NO entregar features a medias.** "No se revisarán funcionalidades parciales, ni funcionalidades no integradas" (regla 6).
- ❌ **NO dejar excepciones sin manejar** que aparezcan durante la ejecución. Cada una cuesta **-2 puntos** (regla 12).
- ❌ **NO hardcodear el número de clientes** a 2. Debe soportar al menos 2, pero el código debe ser extensible.
- ❌ **NO asumir que el cliente nunca se desconectará abruptamente.** Manejar `IOException`, `SocketException`, etc.

## 8. Restricciones de calidad de código

- ❌ **NO dejar código comentado** en la entrega final. Si no se usa, borrarlo.
- ❌ **NO usar nombres crípticos** (`a`, `tmp`, `x1`, `data2`). Nombres descriptivos en español.
- ❌ **NO dejar `TODO` o `FIXME`** sin resolver en la entrega.
- ❌ **NO mezclar idiomas** en el código. Todo en español (excepto palabras reservadas del lenguaje).
- ❌ **NO escribir funciones de más de ~50 líneas** sin una razón clara. Refactorizar en funciones más pequeñas.

## 9. Restricciones específicas que la gente suele olvidar

- ❌ **NO olvidar que el cañón del jugador también daña los bunkers** al dispararles desde abajo (regla del juego original, mencionada en el enunciado).
- ❌ **NO olvidar dar +1 vida** al eliminar todos los aliens.
- ❌ **NO olvidar aumentar la velocidad** al iniciar nueva oleada.
- ❌ **NO olvidar que el OVNI da puntos ALEATORIOS**, no fijos (a diferencia de los aliens regulares).
- ❌ **NO olvidar que el espectador es CADA UNO por jugador**, no uno global (ver diagrama: "Ver Jugador 1", "Ver Jugador 2").

## 10. Lista de verificación antes de hacer commit

Antes de cualquier commit significativo, verificar:

- [ ] El código compila sin warnings nuevos.
- [ ] No introduje constantes mágicas en C (todo va a `constants.h`).
- [ ] No introduje lógica de juego en el cliente C.
- [ ] Las nuevas clases Java tienen responsabilidad única.
- [ ] Comenté las partes no obvias en español.
- [ ] Las excepciones nuevas están manejadas explícitamente.
- [ ] Si hice un `malloc`, hay un `free` correspondiente en algún flujo.
- [ ] No hardcodee paths ni puertos.

## 11. En caso de duda

Si dudas si algo viola una restricción:

1. Releer la sección relevante del enunciado.
2. Preferir la solución más conservadora.
3. Documentar la decisión en la bitácora con justificación.
4. En la defensa, estar preparado para explicar la decisión.
