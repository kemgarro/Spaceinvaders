# Rúbrica de evaluación — spaCEinvaders

Extraído de la sección 7 del enunciado oficial. Este documento es **informativo**, sirve para priorizar esfuerzo.

## Distribución de la nota

| Componente | Peso | Notas |
|-----------|------|-------|
| **Código** (proyecto funcional) | **70%** | Dividido entre C (parte cliente/Pico) y Java (servidor). |
| **Documentación** | **20%** | Coherente, completa, acorde al tamaño del proyecto. |
| **Defensa** | **10%** | Todos los integrantes deben participar. |

Adicional al 100% del proyecto:
- **5% Anexo de Atributos** (DI + AC) — entrega separada en TecDigital.

## Regla del 100% funcional ≠ 100% de nota

Cita textual del enunciado:

> "El profesor no sólo evaluará la funcionalidad del proyecto, esto quiere decir que aunque el proyecto este 100% funcional esto no implica una nota de un 100, ya que se evaluarán aspectos de calidad de código, aplicación del paradigma imperativo y orientado a objetos, calidad de documentación interna y externa y trabajo en equipo."

**Implicación:** la calidad importa tanto como la funcionalidad. No descuidar:
- Aplicación correcta del paradigma (imperativo en C, OO en Java).
- Comentarios y nombres claros.
- Patrones de diseño bien usados.
- Documentación interna (comentarios) y externa (documentos).

## Penalizaciones explícitas

| Falta | Penalización |
|-------|-------------|
| Cada excepción no contemplada durante la ejecución | **-2 puntos** |
| Falta entregar documentación | **Nota 0** |
| Falta el punto 3 de documentación (algoritmos) | **Nota 0** |
| Entrega tardía | **Nota 0** |
| Código no compila | **Nota 0** |
| No traer equipos sin avisar | **Nota 0** |
| No usar paradigma correcto en cada lenguaje | **Nota 0** |
| No presentarse a la defensa | **Nota 0** |
| Indicio de copia | **Nota 0** + proceso disciplinario |

## Distribución sugerida de esfuerzo

Con base en pesos y dificultad relativa:

| Componente | % de esfuerzo recomendado |
|-----------|---------------------------|
| Servidor Java (lógica, patrones, threading) | 30% |
| Cliente C (gráficos, sockets, integración) | 25% |
| Pico (firmware + integración UART) | 10% |
| Documentación (incluido bitácora) | 20% |
| Anexo Atributos (DI + AC) | 10% |
| Pruebas, integración, pulido | 5% |

## Documentación obligatoria (sección 6 del enunciado)

Los 9 puntos obligatorios:

1. Manual de usuario (cómo ejecutar).
2. Descripción de estructuras de datos (incluye nodo de lista).
3. **Descripción detallada de algoritmos** (sin esto = nota 0).
4. Problemas sin solución.
5. Plan de actividades por estudiante.
6. Problemas encontrados (con intentos de solución).
7. Conclusiones del proyecto.
8. Recomendaciones del proyecto.
9. Bibliografía.

Más:
- **Bitácora digital** (diario de trabajo, todas las sesiones).
- **Anexo de Atributos** (DI: diseño / AC: aprendizaje continuo).

## En la defensa (10%)

- Todos los integrantes deben participar.
- Máximo 30 minutos por grupo.
- Tener todo listo antes de entrar (no perder tiempo en setup).
- El profesor puede preguntar a cualquier miembro sobre cualquier parte del código.
- Llevar todos los equipos necesarios.

**Preguntas típicas a preparar:**
- ¿Por qué usaron cada uno de los patrones de diseño?
- ¿Cómo manejan la concurrencia entre múltiples clientes?
- ¿Por qué eligieron UART sobre I2C (o viceversa)?
- ¿Qué pasa si un cliente se desconecta abruptamente?
- ¿Cómo se sincroniza el estado entre clientes?
- Explica cómo funciona la detección de 1 vs 2 toques en el Pico.

## Después de la nota

- 3 días hábiles para reclamo (solo si funcionalidad está completa).
