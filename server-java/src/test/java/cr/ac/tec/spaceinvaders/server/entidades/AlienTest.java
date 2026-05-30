package cr.ac.tec.spaceinvaders.server.entidades;

import cr.ac.tec.spaceinvaders.server.util.Config;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas para {@link Alien} y sus subtipos (Squid, Crab, Octopus).
 */
class AlienTest {

    @Test
    @DisplayName("Cada subtipo retorna los puntos y tipo correctos")
    void subtiposPuntosYTipo() {
        Squid s = new Squid("s", 0, 0);
        Crab c = new Crab("c", 0, 0);
        Octopus o = new Octopus("o", 0, 0);

        assertEquals(Config.PUNTOS_SQUID, s.getPuntos());
        assertEquals(Config.PUNTOS_CRAB, c.getPuntos());
        assertEquals(Config.PUNTOS_OCTOPUS, o.getPuntos());

        assertEquals(TipoAlien.SQUID, s.getTipo());
        assertEquals(TipoAlien.CRAB, c.getTipo());
        assertEquals(TipoAlien.OCTOPUS, o.getTipo());
    }

    @Test
    @DisplayName("Dimensiones correctas por subtipo")
    void subtiposDimensiones() {
        Squid s = new Squid("s", 0, 0);
        Crab c = new Crab("c", 0, 0);
        Octopus o = new Octopus("o", 0, 0);

        assertEquals(Config.ANCHO_SQUID, s.getAncho());
        assertEquals(Config.ANCHO_CRAB, c.getAncho());
        assertEquals(Config.ANCHO_OCTOPUS, o.getAncho());
        assertEquals(Config.ALTO_ALIEN, s.getAlto());
        assertEquals(Config.ALTO_ALIEN, c.getAlto());
        assertEquals(Config.ALTO_ALIEN, o.getAlto());
    }

    @Test
    @DisplayName("mover() desplaza X según velocidadX")
    void moverDesplazaPorVelocidadX() {
        Crab c = new Crab("c", 100, 50);
        double xInicial = c.getX();
        double v = c.getVelocidadX();
        c.mover();
        assertEquals(xInicial + v, c.getX(), 1e-9);
    }

    @Test
    @DisplayName("invertirDireccion cambia el signo de velocidadX")
    void invertirDireccion() {
        Squid s = new Squid("s", 0, 0);
        double original = s.getVelocidadX();
        s.invertirDireccion();
        assertEquals(-original, s.getVelocidadX(), 1e-9);
        s.invertirDireccion();
        assertEquals(original, s.getVelocidadX(), 1e-9);
    }

    @Test
    @DisplayName("bajarFila baja Y por ALIENS_PASO_VERTICAL")
    void bajarFilaIncrementaY() {
        Octopus o = new Octopus("o", 0, 100);
        o.bajarFila();
        assertEquals(100 + Config.ALIENS_PASO_VERTICAL, o.getY(), 1e-9);
    }

    @Test
    @DisplayName("acelerar aumenta el módulo, conservando el signo positivo")
    void acelerarConservaSignoPositivo() {
        Crab c = new Crab("c", 0, 0);
        // velocidad inicial = ALIENS_PASO_HORIZONTAL (positivo)
        double inicial = c.getVelocidadX();
        assertTrue(inicial > 0);
        c.acelerar();
        assertEquals(inicial + 1, c.getVelocidadX(), 1e-9);
        c.acelerar();
        assertEquals(inicial + 2, c.getVelocidadX(), 1e-9);
    }

    @Test
    @DisplayName("acelerar aumenta el módulo, conservando el signo negativo")
    void acelerarConservaSignoNegativo() {
        Crab c = new Crab("c", 0, 0);
        c.invertirDireccion(); // ahora velocidad es negativa
        double inicial = c.getVelocidadX();
        assertTrue(inicial < 0);
        c.acelerar();
        // módulo debe aumentar en 1 manteniendo signo negativo => valor restó 1
        assertEquals(inicial - 1, c.getVelocidadX(), 1e-9);
        c.acelerar();
        assertEquals(inicial - 2, c.getVelocidadX(), 1e-9);
    }
}
