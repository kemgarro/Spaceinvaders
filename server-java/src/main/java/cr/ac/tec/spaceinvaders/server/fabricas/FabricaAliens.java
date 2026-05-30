package cr.ac.tec.spaceinvaders.server.fabricas;

import cr.ac.tec.spaceinvaders.server.entidades.Alien;

/**
 * Interfaz del patron Factory Method para crear aliens.
 * Permite tener distintas implementaciones (estandar, futurista, etc.)
 * sin que el GestorOleadas conozca las clases concretas.
 */
public interface FabricaAliens {
    /** Crea un alien apropiado para la fila dada. */
    Alien crearAlien(int fila, double x, double y);
}
