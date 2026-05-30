package cr.ac.tec.spaceinvaders.server.entidades;

import cr.ac.tec.spaceinvaders.server.util.Config;

/**
 * Proyectil (bala) del juego. Las balas son disparadas tanto por los cañones
 * de los jugadores (suben) como por los aliens (bajan).
 *
 * <p>Atributos clave:</p>
 * <ul>
 *   <li>{@link Direccion}: indica si la bala viaja {@code ARRIBA} (jugador) o
 *       {@code ABAJO} (alien). Cada dirección usa su propia velocidad de
 *       {@link Config#BALA_CANNON_VELOCIDAD} o {@link Config#BALA_ALIEN_VELOCIDAD}.</li>
 *   <li>{@code duenioId}: el identificador del jugador que disparó. Si la bala es
 *       de un alien se usa el sentinel {@link #DUENIO_ALIEN}.</li>
 * </ul>
 */
public class Bala extends EntidadJuego {

    /** Dirección de movimiento de la bala. */
    public enum Direccion {
        /** Bala disparada por un cañón. Sube (y disminuye). */
        ARRIBA,
        /** Bala disparada por un alien. Baja (y aumenta). */
        ABAJO
    }

    /** Sentinel usado como {@code duenioId} para balas disparadas por aliens. */
    public static final String DUENIO_ALIEN = "ALIEN";

    private final Direccion direccion;
    private final String duenioId;

    /**
     * Construye una bala.
     *
     * @param id        identificador único.
     * @param x         posición X inicial.
     * @param y         posición Y inicial.
     * @param direccion {@link Direccion#ARRIBA} si es del jugador, {@link Direccion#ABAJO} si es de alien.
     * @param duenioId  jugadorId del autor, o {@link #DUENIO_ALIEN} si la disparó un alien.
     */
    public Bala(String id, double x, double y, Direccion direccion, String duenioId) {
        super(id, x, y, Config.ANCHO_BALA, Config.ALTO_BALA);
        this.direccion = direccion;
        this.duenioId = duenioId;
    }

    /**
     * Avanza la bala un tick. Las balas de jugador suben con velocidad
     * {@link Config#BALA_CANNON_VELOCIDAD}; las de alien bajan con velocidad
     * {@link Config#BALA_ALIEN_VELOCIDAD}.
     */
    @Override
    public void mover() {
        double velocidad = (direccion == Direccion.ARRIBA)
            ? -Config.BALA_CANNON_VELOCIDAD
            : Config.BALA_ALIEN_VELOCIDAD;
        y += velocidad;
    }

    /** @return true si la bala pertenece a un jugador (no a un alien). */
    public boolean esDelJugador() {
        return !DUENIO_ALIEN.equals(duenioId);
    }

    /** @return dirección de movimiento. */
    public Direccion getDireccion() {
        return direccion;
    }

    /** @return id del dueño (jugadorId o {@link #DUENIO_ALIEN}). */
    public String getDuenioId() {
        return duenioId;
    }
}
