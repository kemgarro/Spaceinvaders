package cr.ac.tec.spaceinvaders.server.entidades;

import cr.ac.tec.spaceinvaders.server.util.Config;

/**
 * Clase base abstracta para los aliens (invasores) del juego.
 *
 * <p>Los aliens se mueven todos juntos como un bloque: avanzan horizontalmente
 * en una dirección, y cuando el bloque toca un borde, invierten dirección y
 * bajan una fila. La velocidad del bloque también aumenta a medida que se
 * eliminan aliens o avanzan oleadas.</p>
 *
 * <p>Esta clase encapsula la mecánica de movimiento individual:</p>
 * <ul>
 *   <li>{@link #mover()} desplaza al alien por {@code velocidadX} en X.</li>
 *   <li>{@link #invertirDireccion()} cambia el signo de {@code velocidadX}.</li>
 *   <li>{@link #bajarFila()} baja al alien {@code Config.ALIENS_PASO_VERTICAL}.</li>
 *   <li>{@link #acelerar()} aumenta el módulo de la velocidad en 1, conservando
 *       el signo.</li>
 * </ul>
 *
 * <p>Las subclases concretas ({@code Squid}, {@code Crab}, {@code Octopus}) aportan
 * los puntos y el tipo correspondiente.</p>
 */
public abstract class Alien extends EntidadJuego {

    /** Velocidad horizontal por tick. El signo indica la dirección actual. */
    protected double velocidadX;

    /**
     * Construye un alien con velocidad horizontal inicial igual a
     * {@link Config#ALIENS_PASO_HORIZONTAL} en dirección positiva.
     */
    protected Alien(String id, double x, double y, int ancho, int alto) {
        super(id, x, y, ancho, alto);
        this.velocidadX = Config.ALIENS_PASO_HORIZONTAL;
    }

    /** @return puntos otorgados al destruir este alien. */
    public abstract int getPuntos();

    /** @return tipo del alien (SQUID, CRAB, OCTOPUS). */
    public abstract TipoAlien getTipo();

    /** Desplaza al alien un paso horizontal en la dirección actual. */
    @Override
    public void mover() {
        x += velocidadX;
    }

    /** Invierte la dirección horizontal del alien. */
    public void invertirDireccion() {
        velocidadX = -velocidadX;
    }

    /** Baja al alien una fila vertical (constante de Config). */
    public void bajarFila() {
        y += Config.ALIENS_PASO_VERTICAL;
    }

    /**
     * Aumenta el módulo de la velocidad en 1, conservando el signo (la dirección
     * actual). Sirve para que el bloque de aliens acelere a medida que avanza
     * la partida.
     */
    public void acelerar() {
        velocidadX += (velocidadX > 0) ? 1 : -1;
    }

    /** @return velocidad horizontal actual (con signo). */
    public double getVelocidadX() {
        return velocidadX;
    }
}
