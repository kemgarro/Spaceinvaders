package cr.ac.tec.spaceinvaders.server.fabricas;

import cr.ac.tec.spaceinvaders.server.entidades.Alien;
import cr.ac.tec.spaceinvaders.server.entidades.Crab;
import cr.ac.tec.spaceinvaders.server.entidades.Octopus;
import cr.ac.tec.spaceinvaders.server.entidades.Ovni;
import cr.ac.tec.spaceinvaders.server.entidades.Squid;
import cr.ac.tec.spaceinvaders.server.util.Config;
import cr.ac.tec.spaceinvaders.server.util.GeneradorIds;

/**
 * Implementacion estandar de {@link FabricaAliens}.
 *
 * <p>Concentra la creacion de aliens y OVNIs (patron <em>Factory Method</em>):
 * encapsula la eleccion del tipo concreto ({@link Squid}, {@link Crab},
 * {@link Octopus}, {@link Ovni}) segun la fila o el puntaje solicitado.</p>
 */
public class FabricaAliensEstandar implements FabricaAliens {

    /**
     * Crea un alien apropiado para la fila indicada en la formacion 5x11
     * clasica de Space Invaders:
     * <ul>
     *   <li>fila 0: {@link Squid} (10 puntos)</li>
     *   <li>filas 1-2: {@link Crab} (20 puntos)</li>
     *   <li>fila >= 3: {@link Octopus} (40 puntos)</li>
     * </ul>
     */
    @Override
    public Alien crearAlien(int fila, double x, double y) {
        String id = GeneradorIds.siguiente("A");
        if (fila == 0)      return new Squid(id, x, y);
        else if (fila < 3)  return new Crab(id, x, y);
        else                return new Octopus(id, x, y);
    }

    /** Variante para el comando admin "Crear (X, Y, Pts)". */
    public Alien crearPorPuntos(double x, double y, int puntos) {
        String id = GeneradorIds.siguiente("A");
        return switch (puntos) {
            case Config.PUNTOS_SQUID   -> new Squid(id, x, y);
            case Config.PUNTOS_CRAB    -> new Crab(id, x, y);
            case Config.PUNTOS_OCTOPUS -> new Octopus(id, x, y);
            default -> throw new IllegalArgumentException("Puntos invalidos: " + puntos);
        };
    }

    /** Crea un OVNI. */
    public Ovni crearOvni(double x, double y, Ovni.Direccion dir, int puntosBase) {
        return new Ovni(GeneradorIds.siguiente("U"), x, y, dir, puntosBase);
    }
}
