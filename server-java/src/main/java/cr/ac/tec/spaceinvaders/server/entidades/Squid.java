package cr.ac.tec.spaceinvaders.server.entidades;

import cr.ac.tec.spaceinvaders.server.util.Config;

/**
 * Alien tipo "calamar". Es el más pequeño y vale {@link Config#PUNTOS_SQUID} puntos.
 */
public class Squid extends Alien {

    /**
     * Construye un Squid en la posición indicada con sus dimensiones de Config.
     *
     * @param id identificador único.
     * @param x  posición X inicial.
     * @param y  posición Y inicial.
     */
    public Squid(String id, double x, double y) {
        super(id, x, y, Config.ANCHO_SQUID, Config.ALTO_ALIEN);
    }

    @Override
    public int getPuntos() {
        return Config.PUNTOS_SQUID;
    }

    @Override
    public TipoAlien getTipo() {
        return TipoAlien.SQUID;
    }
}
