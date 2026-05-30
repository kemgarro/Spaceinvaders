package cr.ac.tec.spaceinvaders.server.entidades;

import java.util.Objects;

/**
 * Clase base abstracta para todas las entidades del juego.
 *
 * <p>Una entidad del juego es cualquier objeto con posición, dimensiones y estado
 * de "vivo/destruido" dentro del campo de juego (aliens, balas, cañones, bunkers,
 * OVNIs). Esta clase concentra el comportamiento común:</p>
 *
 * <ul>
 *   <li>Identificación única por {@code id} (string) para que pueda ser referenciada
 *       desde estructuras propias como la {@code ListaEnlazada}.</li>
 *   <li>Posición {@code (x, y)} en coordenadas de doble precisión (subpíxeles para
 *       movimiento suave).</li>
 *   <li>Dimensiones {@code (ancho, alto)} en píxeles, inmutables tras la creación.</li>
 *   <li>Detección de colisiones AABB ({@link #colisionaCon(EntidadJuego)}). La fórmula
 *       usa comparaciones estrictas {@code <} y {@code >}, por lo que bordes que
 *       apenas se tocan NO se consideran colisión.</li>
 *   <li>{@link #equals(Object)} y {@link #hashCode()} basados únicamente en el id,
 *       permitiendo que estructuras como {@code ListaEnlazada.eliminar} encuentren la
 *       instancia por identidad lógica.</li>
 * </ul>
 *
 * <p>Cada subclase concreta implementa {@link #mover()} con la regla específica de
 * actualización por tick (los aliens se mueven en bloque, el cañón solo por input,
 * las balas en línea recta, los bunkers son estáticos, etc.).</p>
 */
public abstract class EntidadJuego {

    /** Identificador único de la entidad. Usado para igualdad y referencias remotas. */
    protected final String id;

    /** Coordenada X del borde izquierdo de la entidad. */
    protected double x;

    /** Coordenada Y del borde superior de la entidad. */
    protected double y;

    /** Ancho de la entidad en píxeles. Inmutable. */
    protected final int ancho;

    /** Alto de la entidad en píxeles. Inmutable. */
    protected final int alto;

    /** Indica si la entidad sigue activa en el juego. */
    protected boolean vivo;

    /**
     * Construye una entidad en la posición indicada y la marca como viva.
     *
     * @param id    identificador único.
     * @param x     posición X inicial.
     * @param y     posición Y inicial.
     * @param ancho ancho en píxeles.
     * @param alto  alto en píxeles.
     */
    protected EntidadJuego(String id, double x, double y, int ancho, int alto) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.ancho = ancho;
        this.alto = alto;
        this.vivo = true;
    }

    /**
     * Actualiza la posición/estado de la entidad correspondiente a un tick del juego.
     * Cada subclase define su propia regla.
     */
    public abstract void mover();

    /**
     * Detección de colisión por caja envolvente alineada a ejes (AABB).
     *
     * <p>Dos rectángulos se consideran colisionando si y solo si hay solapamiento
     * estricto en ambos ejes. Bordes que se tocan exactamente NO colisionan.</p>
     *
     * @param otra otra entidad a comparar.
     * @return true si los rectángulos se solapan estrictamente.
     */
    public boolean colisionaCon(EntidadJuego otra) {
        return x < otra.x + otra.ancho
            && x + ancho > otra.x
            && y < otra.y + otra.alto
            && y + alto > otra.y;
    }

    /** @return identificador único. */
    public String getId() {
        return id;
    }

    /** @return coordenada X actual. */
    public double getX() {
        return x;
    }

    /** @return coordenada Y actual. */
    public double getY() {
        return y;
    }

    /** @return ancho en píxeles. */
    public int getAncho() {
        return ancho;
    }

    /** @return alto en píxeles. */
    public int getAlto() {
        return alto;
    }

    /** @return true si la entidad sigue viva. */
    public boolean estaVivo() {
        return vivo;
    }

    /** Marca la entidad como destruida. No se "revive". */
    public void destruir() {
        this.vivo = false;
    }

    /**
     * Igualdad basada únicamente en el identificador. Permite que estructuras
     * propias localicen y eliminen entidades por id.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof EntidadJuego)) {
            return false;
        }
        EntidadJuego otra = (EntidadJuego) o;
        return Objects.equals(this.id, otra.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
