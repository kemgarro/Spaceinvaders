package cr.ac.tec.spaceinvaders.server.util;

/**
 * Configuración centralizada del servidor spaCEinvaders.
 *
 * <p>Esta clase contiene TODAS las constantes del proyecto agrupadas por categoría.
 * Todas las constantes son {@code public static final} para evitar instancias y modificaciones.</p>
 *
 * <p>ADAPTADO DEL PROYECTO DonCEyKongJr.
 * Constantes ajustadas a Space Invaders:
 * - Puntajes de aliens (10, 20, 40)
 * - Soporte para 2 jugadores (mínimo requerido)
 * - Dimensiones de campo apropiadas
 * - Velocidades iniciales y reglas de aumento</p>
 */
public class Config {

    // ============================================================
    // RED
    // ============================================================

    /** Puerto TCP por defecto del servidor. */
    public static final int PUERTO_DEFAULT = 5555;

    /** Tamaño máximo del buffer de mensajes. */
    public static final int BUFFER_SIZE = 8192;

    // ============================================================
    // CLIENTES
    // ============================================================

    /**
     * Número máximo de jugadores soportados.
     * El enunciado requiere mínimo 2 jugadores simultáneos.
     */
    public static final int MAX_JUGADORES = 4;

    /** Número máximo de espectadores por jugador. */
    public static final int MAX_ESPECTADORES_POR_JUGADOR = 2;

    // ============================================================
    // GAME LOOP
    // ============================================================

    /**
     * Frecuencia de actualización del juego en ticks por segundo.
     * 20 TPS = 50ms por tick. Suficiente para Space Invaders.
     */
    public static final int TICKS_POR_SEGUNDO = 20;

    /** Intervalo entre ticks en milisegundos. */
    public static final long INTERVALO_TICK_MS = 1000 / TICKS_POR_SEGUNDO;

    // ============================================================
    // JUGADOR
    // ============================================================

    /**
     * Vidas iniciales del jugador.
     * Requisito explícito del enunciado: "iniciara con 3 vidas".
     */
    public static final int VIDAS_INICIALES = 3;

    /** Velocidad del cañón en píxeles por tick. */
    public static final double CANNON_VELOCIDAD = 5.0;

    // ============================================================
    // ALIENS — PUNTAJES
    // ============================================================

    /**
     * Puntos por destruir un alien tipo "calamar" (squid).
     * Requisito del enunciado.
     */
    public static final int PUNTOS_SQUID = 10;

    /**
     * Puntos por destruir un alien tipo "cangrejo" (crab).
     * Requisito del enunciado.
     */
    public static final int PUNTOS_CRAB = 20;

    /**
     * Puntos por destruir un alien tipo "pulpo" (octopus).
     * Requisito del enunciado.
     */
    public static final int PUNTOS_OCTOPUS = 40;

    // ============================================================
    // ALIENS — MOVIMIENTO Y VELOCIDAD
    // ============================================================

    /** Intervalo inicial de movimiento de aliens en milisegundos. */
    public static final long ALIENS_INTERVALO_BASE_MS = 800;

    /** Intervalo mínimo (más rápido) que pueden alcanzar los aliens. */
    public static final long ALIENS_INTERVALO_MIN_MS = 50;

    /**
     * Reducción del intervalo base por cada nueva oleada (en %).
     * Si una oleada tiene intervalo X, la siguiente tendrá X * (1 - este valor).
     */
    public static final double ALIENS_REDUCCION_POR_OLEADA = 0.15;

    /** Píxeles que se mueven los aliens en cada paso horizontal. */
    public static final int ALIENS_PASO_HORIZONTAL = 10;

    /** Píxeles que bajan los aliens cuando llegan al borde. */
    public static final int ALIENS_PASO_VERTICAL = 20;

    // ============================================================
    // OVNI
    // ============================================================

    /** Intervalo mínimo entre apariciones de OVNI (segundos). */
    public static final double OVNI_INTERVALO_MIN_SEG = 20.0;

    /** Intervalo máximo entre apariciones de OVNI (segundos). */
    public static final double OVNI_INTERVALO_MAX_SEG = 30.0;

    /** Velocidad horizontal del OVNI en píxeles por tick. */
    public static final double OVNI_VELOCIDAD = 2.0;

    /**
     * Multiplicador mínimo del valor base del OVNI (puntos aleatorios).
     * El valor base lo ingresa el admin; el OVNI da entre base*MIN y base*MAX.
     */
    public static final double OVNI_PUNTOS_MULT_MIN = 0.5;

    /** Multiplicador máximo del valor base del OVNI. */
    public static final double OVNI_PUNTOS_MULT_MAX = 2.0;

    // ============================================================
    // BUNKERS
    // ============================================================

    /**
     * Número de bunkers de protección.
     * Requisito del enunciado: "cuatro escudos de protección terrestre".
     */
    public static final int NUM_BUNKERS = 4;

    /** Estado inicial de un bunker (% de salud). */
    public static final int BUNKER_SALUD_INTACTO = 100;

    /** Estados intermedios de daño visible. */
    public static final int BUNKER_SALUD_DANIADO = 70;
    public static final int BUNKER_SALUD_CRITICO = 40;
    public static final int BUNKER_SALUD_DESTRUIDO = 0;

    /** Daño que un disparo causa al bunker (% de salud). */
    public static final int BUNKER_DANIO_POR_DISPARO = 10;

    // ============================================================
    // BALAS
    // ============================================================

    /** Velocidad de bala del cañón en píxeles por tick. */
    public static final double BALA_CANNON_VELOCIDAD = 8.0;

    /** Velocidad de bala de alien en píxeles por tick. */
    public static final double BALA_ALIEN_VELOCIDAD = 4.0;

    /**
     * Bullets máximos del jugador en pantalla a la vez.
     * En Space Invaders clásico: 1.
     */
    public static final int MAX_BALAS_JUGADOR = 1;

    /** Probabilidad por tick de que un alien dispare. */
    public static final double PROB_DISPARO_ALIEN_POR_TICK = 0.005;

    // ============================================================
    // CAMPO DE JUEGO
    // ============================================================

    /** Ancho del campo de juego en píxeles. */
    public static final int CAMPO_ANCHO = 800;

    /** Alto del campo de juego en píxeles. */
    public static final int CAMPO_ALTO = 600;

    /** Posición Y del cañón (línea de defensa). */
    public static final int CANNON_Y = 550;

    // ============================================================
    // DIMENSIONES DE ENTIDADES
    // ============================================================

    /** Ancho del alien tipo "calamar" (squid) en píxeles. */
    public static final int ANCHO_SQUID = 24;

    /** Alto común para todos los aliens en píxeles. */
    public static final int ALTO_ALIEN = 24;

    /** Ancho del alien tipo "cangrejo" (crab) en píxeles. */
    public static final int ANCHO_CRAB = 32;

    /** Ancho del alien tipo "pulpo" (octopus) en píxeles. */
    public static final int ANCHO_OCTOPUS = 36;

    /** Ancho del OVNI (nave nodriza) en píxeles. */
    public static final int ANCHO_OVNI = 40;

    /** Alto del OVNI en píxeles. */
    public static final int ALTO_OVNI = 20;

    /** Ancho del cañón (jugador) en píxeles. */
    public static final int ANCHO_CANNON = 40;

    /** Alto del cañón (jugador) en píxeles. */
    public static final int ALTO_CANNON = 20;

    /** Ancho de una bala en píxeles. */
    public static final int ANCHO_BALA = 3;

    /** Alto de una bala en píxeles. */
    public static final int ALTO_BALA = 10;

    /** Ancho de un bunker (escudo) en píxeles. */
    public static final int ANCHO_BUNKER = 60;

    /** Alto de un bunker (escudo) en píxeles. */
    public static final int ALTO_BUNKER = 40;

    /** Constructor privado para prevenir instanciación. */
    private Config() {
        throw new AssertionError("No se debe instanciar la clase Config");
    }
}
