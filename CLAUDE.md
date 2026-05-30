# spaCEinvaders — Proyecto CE3104 / CE1106

Este es un proyecto académico del Instituto Tecnológico de Costa Rica, curso de Lenguajes, Compiladores e Intérpretes / Paradigmas de Programación. La fecha de entrega es el **11 de Junio de 2026**.

## Resumen del proyecto

Implementación distribuida del videojuego clásico Space Invaders con:
- **Servidor en Java** (paradigma orientado a objetos) que mantiene toda la lógica del juego.
- **Cliente jugador en C** (paradigma imperativo) con interfaz gráfica.
- **Cliente espectador** que solo observa la partida.
- **Control físico** con Raspberry Pi Pico programado en C, comunicado por UART.
- **Comunicación** servidor-cliente vía Sockets TCP.

El servidor debe soportar **mínimo 2 clientes jugadores simultáneos** + sus espectadores correspondientes.

## Cómo usar las skills de este proyecto

Antes de generar o modificar código, **siempre consultar primero** las skills relevantes ubicadas en `.claude/skills/`. Cada skill cubre un aspecto distinto del proyecto:

| Skill | Cuándo consultarla |
|-------|-------------------|
| `requirements/SKILL.md` | Antes de cualquier tarea — lista completa de requerimientos funcionales y no funcionales |
| `constraints/SKILL.md` | **SIEMPRE** antes de escribir código — restricciones absolutas que invalidan el proyecto si se violan |
| `project-structure/SKILL.md` | Al crear archivos o carpetas nuevas |
| `game-mechanics/SKILL.md` | Al implementar reglas del juego (puntuaciones, vidas, oleadas, OVNI, bunkers) |
| `network-protocol/SKILL.md` | Al implementar mensajes entre servidor y clientes |
| `java-server/SKILL.md` | Al trabajar en el servidor Java (patrones de diseño, clases, paquetes) |
| `c-client/SKILL.md` | Al trabajar en los clientes C (jugador o espectador) |
| `pico-controller/SKILL.md` | Al programar el firmware del Raspberry Pi Pico |
| `reusable-base-code/SKILL.md` | **ANTES de implementar cualquier archivo del servidor Java** — verifica si existe versión reutilizable del proyecto previo DonCEyKongJr |

## Flujo de trabajo esperado

1. **Leer la skill aplicable completamente** antes de generar código.
2. Si la tarea involucra varios componentes, leer todas las skills relacionadas.
3. **Verificar contra `constraints/SKILL.md`** que la solución propuesta no viole restricciones.
4. Generar código siguiendo las convenciones indicadas.
5. Comentar el código en español (el proyecto es en español y el profesor evalúa documentación interna).

## Prioridades absolutas

1. **El código debe compilar.** Si no compila, la nota es 0 (regla 10.4 del enunciado).
2. **Respetar el paradigma asignado.** C = imperativo. Java = orientado a objetos. Mezclarlos da nota 0 (regla 10.6).
3. **Manejar excepciones.** Cada excepción no contemplada cuesta 2 puntos de la nota final.
4. **No se revisan funcionalidades parciales.** Es mejor menos features bien terminadas.

## Reglas sobre commits y autoría

**IMPORTANTE — Reglas para Claude Code al hacer commits:**

1. **NO agregar firmas de Claude** en los mensajes de commit. Esto significa:
   - NO incluir líneas como `🤖 Generated with [Claude Code]`.
   - NO incluir `Co-Authored-By: Claude <noreply@anthropic.com>`.
   - NO mencionar "Claude", "AI", "GPT", "Anthropic" o términos similares en mensajes de commit.

2. **Mensajes de commit en español**, descriptivos y en primera persona singular o impersonal:
   - ✅ "implemento detección de colisiones AABB"
   - ✅ "agrego clase Alien con sus subtipos"
   - ✅ "corrijo race condition en notificación de observers"
   - ❌ "fix" / "wip" / "updates"
   - ❌ "Generated with Claude Code"

3. **Commits pequeños y frecuentes.** Mejor 5 commits de 50 líneas que 1 commit de 250 líneas.

4. **Antes de cualquier commit, mostrar el mensaje propuesto** para que el usuario lo apruebe o modifique.

5. **El autor del commit es el usuario** (configurado en su `git config`), no Claude.

El uso de IA se declara apropiadamente en la documentación final del proyecto, no en cada commit.


## Información del equipo

- Integrantes: 3 personas.
- Experiencia previa: Sockets en Java, Raspberry Pi Pico.
- Áreas a reforzar: gráficos en C, patrones de diseño OOP en Java.

## Idioma

Todo el código, comentarios, nombres de variables relevantes y documentación deben estar en **español** salvo palabras reservadas del lenguaje.
