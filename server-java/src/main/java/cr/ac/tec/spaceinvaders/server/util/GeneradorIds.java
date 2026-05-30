package cr.ac.tec.spaceinvaders.server.util;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Generador thread-safe de identificadores únicos para entidades del juego.
 *
 * <p>Mantiene un contador atómico monotónicamente creciente para construir
 * cadenas con la forma {@code <prefijo>_<n>} (por ejemplo {@code "A_42"}).
 * El contador es compartido entre todos los prefijos, garantizando que dos
 * llamadas concurrentes nunca produzcan el mismo identificador.</p>
 *
 * <p>Prefijos usados en el motor:</p>
 * <ul>
 *   <li>{@code "A"}  — aliens.</li>
 *   <li>{@code "B"}  — balas.</li>
 *   <li>{@code "BK"} — bunkers.</li>
 *   <li>{@code "C"}  — cañones.</li>
 *   <li>{@code "U"}  — OVNIs.</li>
 * </ul>
 */
public final class GeneradorIds {

    /** Contador global atómico. */
    private static final AtomicLong CONTADOR = new AtomicLong(0);

    /** No instanciable. */
    private GeneradorIds() {
        throw new AssertionError("No se debe instanciar la clase GeneradorIds");
    }

    /**
     * Retorna el siguiente identificador con el prefijo indicado.
     *
     * @param prefijo prefijo de la entidad (por ejemplo {@code "A"}, {@code "B"}).
     * @return cadena {@code prefijo + "_" + n} con un entero único.
     */
    public static String siguiente(String prefijo) {
        return prefijo + "_" + CONTADOR.incrementAndGet();
    }
}
