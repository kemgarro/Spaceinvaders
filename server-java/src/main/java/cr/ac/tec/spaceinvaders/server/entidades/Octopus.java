package cr.ac.tec.spaceinvaders.server.entidades;

import cr.ac.tec.spaceinvaders.server.util.Config;

/**
 * Alien tipo "pulpo". Es el más grande y vale {@link Config#PUNTOS_OCTOPUS} puntos.
 */
public class Octopus extends Alien {

    /**
     * Construye un Octopus en la posición indicada con sus dimensiones de Config.
     *
     * @param id identificador único.
     * @param x  posición X inicial.
     * @param y  posición Y inicial.
     */
    public Octopus(String id, double x, double y) {
        super(id, x, y, Config.ANCHO_OCTOPUS, Config.ALTO_ALIEN);
    }

    @Override
    public int getPuntos() {
        return Config.PUNTOS_OCTOPUS;
    }

    @Override
    public TipoAlien getTipo() {
        return TipoAlien.OCTOPUS;
    }
}
