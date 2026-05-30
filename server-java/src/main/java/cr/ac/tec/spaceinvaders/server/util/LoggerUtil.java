package cr.ac.tec.spaceinvaders.server.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Utilidad de logging simplificada para el servidor spaCEinvaders.
 *
 * <p>Esta clase proporciona métodos estáticos para registrar eventos y mensajes
 * del servidor con timestamps automáticos y niveles de severidad diferenciados.</p>
 *
 * <p><b>Niveles de logging soportados:</b></p>
 * <ul>
 *   <li><b>INFO:</b> Eventos normales y flujo del programa (salida estándar)</li>
 *   <li><b>WARN:</b> Advertencias que no detienen el programa (salida estándar)</li>
 *   <li><b>ERROR:</b> Errores críticos y excepciones (salida de error)</li>
 *   <li><b>DEBUG:</b> Información detallada para debugging (salida estándar)</li>
 * </ul>
 *
 * <p><b>Formato de salida:</b></p>
 * <pre>
 * [YYYY-MM-DD HH:mm:ss] [NIVEL] mensaje
 * Ejemplo: [2025-01-15 14:32:10] [INFO] servidor iniciado
 * </pre>
 *
 * <p><b>Características:</b></p>
 * <ul>
 *   <li>Thread-safe: Los métodos println/err.println son sincronizados</li>
 *   <li>Sin dependencias externas: Usa solo System.out y System.err</li>
 *   <li>Timestamps precisos con formato ISO-8601 simplificado</li>
 *   <li>Diferenciación visual por nivel (INFO/WARN/ERROR/DEBUG)</li>
 * </ul>
 *
 * <p><b>Diseño simplificado:</b></p>
 * Esta implementación deliberadamente evita frameworks de logging complejos
 * (Log4j, SLF4J) para mantener el proyecto ligero y sin dependencias adicionales.
 * Para producción a gran escala, se recomienda migrar a un framework robusto.
 *
 * <p><b>Ejemplo de uso:</b></p>
 * <pre>{@code
 * // Registrar inicio del servidor
 * LoggerUtil.info("Servidor iniciado en puerto " + puerto);
 *
 * // Advertencia sobre configuración
 * LoggerUtil.warning("No se encontró archivo de configuración, usando valores por defecto");
 *
 * // Error crítico
 * LoggerUtil.error("No se pudo conectar a la base de datos: " + e.getMessage());
 *
 * // Información de debugging
 * LoggerUtil.debug("Estado del jugador: x=" + x + ", y=" + y + ", vidas=" + vidas);
 * }</pre>
 *
 * <p><b>Consideraciones de rendimiento:</b></p>
 * <ul>
 *   <li>LocalDateTime.now() tiene overhead (~1-2 microsegundos)</li>
 *   <li>El formateo de fecha tiene overhead (~5-10 microsegundos)</li>
 *   <li>Para logging de alta frecuencia (>10,000 msg/s), considerar caching del timestamp</li>
 *   <li>System.out tiene buffer interno, pero puede bloquear en I/O intensivo</li>
 * </ul>
 *
 * @author spaCEinvaders Team
 * @version 1.0
 * @see System#out
 * @see System#err
 * @see LocalDateTime
 */
public class LoggerUtil {

    /**
     * Formateador de fecha/hora para los timestamps de los logs.
     *
     * <p>Patrón: "yyyy-MM-dd HH:mm:ss" (año-mes-día hora:minuto:segundo)</p>
     * <p>Ejemplo: "2025-01-15 14:32:10"</p>
     *
     * <p><b>Thread-safety:</b> DateTimeFormatter es inmutable y thread-safe,
     * por lo que puede ser compartido entre múltiples hilos de forma segura.</p>
     *
     * @see DateTimeFormatter#ofPattern(String)
     */
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Constructor privado para prevenir instanciación.
     *
     * <p>Esta es una clase utilitaria que solo contiene métodos estáticos.
     * No debe ser instanciada.</p>
     *
     * @throws AssertionError Si se intenta instanciar mediante reflexión
     */
    private LoggerUtil() {
        throw new AssertionError("No se debe instanciar la clase LoggerUtil");
    }

    /**
     * Registra un mensaje informativo (nivel INFO).
     *
     * <p>Utilizado para eventos normales del flujo del programa, como:</p>
     * <ul>
     *   <li>Inicio y cierre de componentes</li>
     *   <li>Conexiones de clientes</li>
     *   <li>Cambios de estado del juego</li>
     *   <li>Operaciones completadas exitosamente</li>
     * </ul>
     *
     * <p><b>Salida:</b> Imprime en {@link System#out} (salida estándar)</p>
     *
     * <p><b>Formato:</b></p>
     * <pre>
     * [2025-01-15 14:32:10] [INFO] mensaje
     * </pre>
     *
     * <p><b>Thread-safety:</b> Este método es thread-safe porque System.out.println()
     * está sincronizado internamente.</p>
     *
     * <p><b>Ejemplo de uso:</b></p>
     * <pre>{@code
     * LoggerUtil.info("GameManager inicializado");
     * LoggerUtil.info("Cliente conectado: " + clientId);
     * LoggerUtil.info("Jugador alcanzó el objetivo");
     * }</pre>
     *
     * @param mensaje El mensaje a registrar. Si es null, se imprime "null"
     * @see #warning(String)
     * @see #error(String)
     * @see #debug(String)
     */
    public static void info(String mensaje) {
        // Obtener timestamp actual y formatearlo
        // LocalDateTime.now() captura el momento exacto
        // FORMATTER.format() convierte a string "yyyy-MM-dd HH:mm:ss"
        System.out.println("[" + LocalDateTime.now().format(FORMATTER) + "] [INFO] " + mensaje);
    }

    /**
     * Registra un mensaje de advertencia (nivel WARN).
     *
     * <p>Utilizado para situaciones anómalas que no detienen el programa, como:</p>
     * <ul>
     *   <li>Configuración faltante o inválida (usando valores por defecto)</li>
     *   <li>Recursos casi agotados (memoria, conexiones)</li>
     *   <li>Operaciones que fallaron pero tienen fallback</li>
     *   <li>Inputs de usuario sospechosos pero válidos</li>
     *   <li>Deprecated features siendo utilizadas</li>
     * </ul>
     *
     * <p><b>Salida:</b> Imprime en {@link System#out} (salida estándar)</p>
     *
     * <p><b>Formato:</b></p>
     * <pre>
     * [2025-01-15 14:32:10] [WARN] mensaje
     * </pre>
     *
     * <p><b>Cuándo usar WARN vs ERROR:</b></p>
     * <ul>
     *   <li><b>WARN:</b> El programa puede continuar normalmente</li>
     *   <li><b>ERROR:</b> Algo falló y requiere atención inmediata</li>
     * </ul>
     *
     * <p><b>Ejemplo de uso:</b></p>
     * <pre>{@code
     * LoggerUtil.warning("El GameLoop ya está ejecutándose");
     * LoggerUtil.warning("No se pudo incrementar dificultad: " + e.getMessage());
     * LoggerUtil.warning("Cliente intentó crear más jugadores del máximo permitido");
     * }</pre>
     *
     * @param mensaje El mensaje de advertencia. Si es null, se imprime "null"
     * @see #info(String)
     * @see #error(String)
     */
    public static void warning(String mensaje) {
        // Similar a info() pero con etiqueta [WARN]
        System.out.println("[" + LocalDateTime.now().format(FORMATTER) + "] [WARN] " + mensaje);
    }

    /**
     * Registra un mensaje de error (nivel ERROR).
     *
     * <p>Utilizado para errores críticos y excepciones que requieren atención, como:</p>
     * <ul>
     *   <li>Excepciones no manejadas</li>
     *   <li>Fallos de red o I/O</li>
     *   <li>Violaciones de invariantes del sistema</li>
     *   <li>Operaciones críticas que fallaron</li>
     *   <li>Corrupción de datos detectada</li>
     * </ul>
     *
     * <p><b>Salida:</b> Imprime en {@link System#err} (salida de error estándar)</p>
     *
     * <p><b>Formato:</b></p>
     * <pre>
     * [2025-01-15 14:32:10] [ERROR] mensaje
     * </pre>
     *
     * <p><b>Diferencia con System.out:</b></p>
     * <ul>
     *   <li>System.err no tiene buffering (se imprime inmediatamente)</li>
     *   <li>Puede ser redirigido independientemente de System.out</li>
     *   <li>Típicamente se muestra en rojo en consolas que soportan colores</li>
     *   <li>Permite filtrar errores en logs del sistema</li>
     * </ul>
     *
     * <p><b>Mejores prácticas:</b></p>
     * <ul>
     *   <li>Incluir información de contexto (qué operación falló)</li>
     *   <li>Incluir el mensaje de la excepción si aplica</li>
     *   <li>Considerar usar printStackTrace() después del log para stack trace completo</li>
     * </ul>
     *
     * <p><b>Ejemplo de uso:</b></p>
     * <pre>{@code
     * LoggerUtil.error("Error en GameLoop: " + e.getMessage());
     * LoggerUtil.error("No se pudo cerrar el socket del cliente " + clientId);
     * LoggerUtil.error("Hilo del servidor interrumpido inesperadamente");
     * }</pre>
     *
     * @param mensaje El mensaje de error. Si es null, se imprime "null"
     * @see #warning(String)
     * @see System#err
     */
    public static void error(String mensaje) {
        // Usar System.err en lugar de System.out para errores
        // err no tiene buffering, se imprime inmediatamente
        System.err.println("[" + LocalDateTime.now().format(FORMATTER) + "] [ERROR] " + mensaje);
    }

    /**
     * Registra un mensaje de depuración (nivel DEBUG).
     *
     * <p>Utilizado para información detallada útil durante desarrollo y debugging, como:</p>
     * <ul>
     *   <li>Estado interno de objetos</li>
     *   <li>Valores de variables en puntos clave</li>
     *   <li>Flujo de ejecución detallado</li>
     *   <li>Resultados de cálculos intermedios</li>
     *   <li>Información de red (paquetes enviados/recibidos)</li>
     * </ul>
     *
     * <p><b>Salida:</b> Imprime en {@link System#out} (salida estándar)</p>
     *
     * <p><b>Formato:</b></p>
     * <pre>
     * [2025-01-15 14:32:10] [DEBUG] mensaje
     * </pre>
     *
     * <p><b>Cuándo usar DEBUG:</b></p>
     * <ul>
     *   <li>Durante desarrollo para rastrear bugs</li>
     *   <li>Para verificar que los cálculos sean correctos</li>
     *   <li>Para entender el flujo de ejecución en casos complejos</li>
     *   <li>Para monitorear el estado del sistema sin afectar INFO</li>
     * </ul>
     *
     * <p><b>Nota de producción:</b></p>
     * En un sistema de logging más robusto, los mensajes DEBUG típicamente
     * se desactivan en producción para reducir overhead y ruido en los logs.
     * Esta implementación simple siempre imprime DEBUG.
     *
     * <p><b>Ejemplo de uso:</b></p>
     * <pre>{@code
     * LoggerUtil.debug("Cocodrilo creado: " + id + " en liana " + lianaId);
     * LoggerUtil.debug("Input ignorado para jugador " + jugadorId + ": " + accion);
     * LoggerUtil.debug("Snapshot del motor: " + snapshot);
     * }</pre>
     *
     * @param mensaje El mensaje de depuración. Si es null, se imprime "null"
     * @see #info(String)
     */
    public static void debug(String mensaje) {
        // Similar a info() pero con etiqueta [DEBUG]
        // En una implementación más avanzada, esto podría ser desactivable
        System.out.println("[" + LocalDateTime.now().format(FORMATTER) + "] [DEBUG] " + mensaje);
    }
}
