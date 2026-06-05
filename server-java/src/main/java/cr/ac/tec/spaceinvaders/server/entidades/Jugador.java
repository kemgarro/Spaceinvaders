package cr.ac.tec.spaceinvaders.server.entidades;

import cr.ac.tec.spaceinvaders.server.util.Config;

/**
 * Representa a un jugador conectado. NO es una {@link EntidadJuego} en sí (no
 * tiene posición ni se dibuja directamente), sino un agregado del estado de
 * juego asociado a un cliente: nombre, puntaje, vidas y su cañón asignado.
 *
 * <p>Reglas de vidas (ver {@code game-mechanics/SKILL.md}):</p>
 * <ul>
 *   <li>Vidas iniciales = {@link Config#VIDAS_INICIALES} (3).</li>
 *   <li>{@link #perderVida()} reduce vidas a un mínimo de 0.</li>
 *   <li>{@link #ganarVida()} aumenta sin tope (premios por puntaje, OVNI, etc.).</li>
 *   <li>{@link #estaVivo()} es true mientras vidas {@code > 0}.</li>
 * </ul>
 *
 * <p>{@link #agregarPuntos(int)} rechaza valores negativos lanzando
 * {@link IllegalArgumentException} para evitar restas accidentales de puntaje.</p>
 */
public class Jugador {

    private final String id;
    private final String nombre;
    private int puntaje;
    private int vidas;
    private Canon canon;

    /**
     * Construye un jugador nuevo con puntaje 0 y vidas = {@link Config#VIDAS_INICIALES}.
     *
     * @param id     identificador único del jugador.
     * @param nombre nombre/alias del jugador.
     */
    public Jugador(String id, String nombre) {
        this.id = id;
        this.nombre = nombre;
        this.puntaje = 0;
        this.vidas = Config.VIDAS_INICIALES;
    }

    /**
     * Suma puntos al puntaje. No se aceptan valores negativos.
     *
     * @param p puntos a sumar (≥ 0).
     * @throws IllegalArgumentException si {@code p < 0}.
     */
    public void agregarPuntos(int p) {
        if (p < 0) {
            throw new IllegalArgumentException("No se pueden agregar puntos negativos: " + p);
        }
        puntaje += p;
    }

    /** Reduce las vidas en 1, sin bajar de 0. */
    public void perderVida() {
        if (vidas > 0) {
            vidas--;
        }
    }

    /** Aumenta las vidas en 1. Sin límite superior (vidas extra por puntaje/OVNI). */
    public void ganarVida() {
        vidas++;
    }

    /**
     * Reinicia el jugador a su estado inicial: puntaje en 0 y vidas en
     * {@link Config#VIDAS_INICIALES}. Se usa cuando el motor recibe la
     * accion RESTART tras un GAME_OVER. NO toca el canon: el cañón se
     * mantiene en la posicion que tenia (el motor decide si reposicionarlo).
     */
    public void reiniciar() {
        this.puntaje = 0;
        this.vidas = Config.VIDAS_INICIALES;
    }

    /** @return true si al jugador le quedan vidas. */
    public boolean estaVivo() {
        return vidas > 0;
    }

    /**
     * Asocia un cañón al jugador. Reemplaza el anterior si ya había uno.
     *
     * @param c cañón a asignar.
     */
    public void asignarCanon(Canon c) {
        this.canon = c;
    }

    /** @return identificador único del jugador. */
    public String getId() {
        return id;
    }

    /** @return nombre del jugador. */
    public String getNombre() {
        return nombre;
    }

    /** @return puntaje acumulado. */
    public int getPuntaje() {
        return puntaje;
    }

    /** @return vidas restantes. */
    public int getVidas() {
        return vidas;
    }

    /** @return cañón asignado al jugador, o {@code null} si aún no se ha asignado. */
    public Canon getCanon() {
        return canon;
    }
}
