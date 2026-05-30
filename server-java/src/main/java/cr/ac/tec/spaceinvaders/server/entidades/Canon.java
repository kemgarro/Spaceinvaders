package cr.ac.tec.spaceinvaders.server.entidades;

import cr.ac.tec.spaceinvaders.server.util.Config;

/**
 * Cañón controlado por un jugador. Se mueve únicamente por input del jugador
 * (no tiene movimiento autónomo en cada tick).
 *
 * <p>El cañón está restringido al ancho del campo: no puede salirse del rango
 * {@code [0, CAMPO_ANCHO - ancho]}.</p>
 *
 * <p>El método {@link #disparar(String)} crea una {@link Bala} dirigida hacia
 * arriba, centrada horizontalmente sobre el cañón, y le asigna el id del jugador
 * como dueño (para resolver puntaje y conteo de balas por jugador).</p>
 */
public class Canon extends EntidadJuego {

    /** Identificador del jugador dueño del cañón. */
    private final String jugadorId;

    /**
     * Construye un cañón para el jugador indicado.
     *
     * @param id        identificador único del cañón (entidad).
     * @param jugadorId identificador del jugador propietario.
     * @param x         posición X inicial.
     * @param y         posición Y inicial.
     */
    public Canon(String id, String jugadorId, double x, double y) {
        super(id, x, y, Config.ANCHO_CANNON, Config.ALTO_CANNON);
        this.jugadorId = jugadorId;
    }

    /** El cañón no se mueve por tick; solo reacciona a input. */
    @Override
    public void mover() {
        // intencionalmente vacío
    }

    /**
     * Desplaza el cañón a la izquierda. No puede salirse del campo (clamp en x=0).
     */
    public void moverIzquierda() {
        x -= Config.CANNON_VELOCIDAD;
        if (x < 0) {
            x = 0;
        }
    }

    /**
     * Desplaza el cañón a la derecha. No puede salirse del campo
     * (clamp en {@code CAMPO_ANCHO - ancho}).
     */
    public void moverDerecha() {
        x += Config.CANNON_VELOCIDAD;
        double maxX = Config.CAMPO_ANCHO - ancho;
        if (x > maxX) {
            x = maxX;
        }
    }

    /**
     * Genera una bala saliendo del centro superior del cañón hacia arriba.
     *
     * @param idBala identificador único asignado a la nueva bala.
     * @return nueva bala con dirección {@link Bala.Direccion#ARRIBA} y dueño = jugadorId.
     */
    public Bala disparar(String idBala) {
        double bx = x + (ancho - Config.ANCHO_BALA) / 2.0;
        double by = y - Config.ALTO_BALA;
        return new Bala(idBala, bx, by, Bala.Direccion.ARRIBA, jugadorId);
    }

    /** @return identificador del jugador propietario. */
    public String getJugadorId() {
        return jugadorId;
    }
}
