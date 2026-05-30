package cr.ac.tec.spaceinvaders.server.entidades;

import cr.ac.tec.spaceinvaders.server.util.Config;

import java.util.Random;

/**
 * La nave nodriza (OVNI) que cruza la parte superior del campo de juego.
 *
 * <p>Mecánica:</p>
 * <ul>
 *   <li>Aparece cada cierto intervalo aleatorio (ver {@code Config.OVNI_INTERVALO_*}).</li>
 *   <li>Cruza la pantalla horizontalmente en una de dos direcciones.</li>
 *   <li>Al ser destruido otorga puntos aleatorios calculados como
 *       {@code puntosBase * uniforme[OVNI_PUNTOS_MULT_MIN, OVNI_PUNTOS_MULT_MAX]},
 *       redondeado al entero más cercano.</li>
 * </ul>
 *
 * <p>El valor base de puntos lo configura el administrador. Para pruebas se ofrece
 * un constructor package-private que recibe un {@link Random} inyectado, de modo
 * que los tests puedan reproducir resultados con una semilla conocida.</p>
 */
public class Ovni extends EntidadJuego {

    /** Dirección horizontal del OVNI. */
    public enum Direccion {
        /** Aparece por la izquierda y se mueve hacia la derecha. */
        IZQUIERDA_A_DERECHA,
        /** Aparece por la derecha y se mueve hacia la izquierda. */
        DERECHA_A_IZQUIERDA
    }

    private final Direccion direccion;
    private final int puntosBase;
    private final Random aleatorio;

    /**
     * Construye un OVNI con un {@link Random} interno por defecto.
     *
     * @param id          identificador único.
     * @param x           posición X inicial.
     * @param y           posición Y inicial.
     * @param direccion   dirección horizontal de movimiento.
     * @param puntosBase  valor base configurado por el administrador.
     */
    public Ovni(String id, double x, double y, Direccion direccion, int puntosBase) {
        this(id, x, y, direccion, puntosBase, new Random());
    }

    /**
     * Constructor package-private que permite inyectar un {@link Random}
     * controlado (semilla fija) para pruebas reproducibles.
     */
    Ovni(String id, double x, double y, Direccion direccion, int puntosBase, Random aleatorio) {
        super(id, x, y, Config.ANCHO_OVNI, Config.ALTO_OVNI);
        this.direccion = direccion;
        this.puntosBase = puntosBase;
        this.aleatorio = aleatorio;
    }

    /**
     * Avanza el OVNI un tick horizontal. El sentido depende de {@link #direccion}.
     */
    @Override
    public void mover() {
        double dx = Config.OVNI_VELOCIDAD
            * (direccion == Direccion.IZQUIERDA_A_DERECHA ? 1 : -1);
        x += dx;
    }

    /**
     * Calcula los puntos otorgados al ser derribado.
     *
     * <p>Resultado: {@code round(puntosBase * uniforme[MULT_MIN, MULT_MAX])}.</p>
     *
     * @return puntos enteros entre {@code round(puntosBase * MULT_MIN)} y
     *         {@code round(puntosBase * MULT_MAX)} inclusive.
     */
    public int otorgarPuntosAleatorios() {
        double rango = Config.OVNI_PUNTOS_MULT_MAX - Config.OVNI_PUNTOS_MULT_MIN;
        double multiplicador = Config.OVNI_PUNTOS_MULT_MIN + aleatorio.nextDouble() * rango;
        return (int) Math.round(puntosBase * multiplicador);
    }

    /** @return valor base de puntos configurado. */
    public int getPuntosBase() {
        return puntosBase;
    }

    /** @return dirección horizontal de movimiento. */
    public Direccion getDireccion() {
        return direccion;
    }
}
