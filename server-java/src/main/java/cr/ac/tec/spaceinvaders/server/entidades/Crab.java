package cr.ac.tec.spaceinvaders.server.entidades;

import cr.ac.tec.spaceinvaders.server.util.Config;

/**
 * Alien tipo "cangrejo". Tamaño intermedio, vale {@link Config#PUNTOS_CRAB} puntos.
 */
public class Crab extends Alien {

    /**
     * Construye un Crab en la posición indicada con sus dimensiones de Config.
     *
     * @param id identificador único.
     * @param x  posición X inicial.
     * @param y  posición Y inicial.
     */
    public Crab(String id, double x, double y) {
        super(id, x, y, Config.ANCHO_CRAB, Config.ALTO_ALIEN);
    }

    @Override
    public int getPuntos() {
        return Config.PUNTOS_CRAB;
    }

    @Override
    public TipoAlien getTipo() {
        return TipoAlien.CRAB;
    }
}
