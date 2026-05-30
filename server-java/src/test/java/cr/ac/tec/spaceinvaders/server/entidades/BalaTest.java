package cr.ac.tec.spaceinvaders.server.entidades;

import cr.ac.tec.spaceinvaders.server.util.Config;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas para {@link Bala}: dirección, velocidad y dueño.
 */
class BalaTest {

    @Test
    @DisplayName("Bala ARRIBA reduce Y por BALA_CANNON_VELOCIDAD")
    void balaArribaReduceY() {
        Bala b = new Bala("b", 100, 200, Bala.Direccion.ARRIBA, "jugador1");
        b.mover();
        assertEquals(200 - Config.BALA_CANNON_VELOCIDAD, b.getY(), 1e-9);
    }

    @Test
    @DisplayName("Bala ABAJO aumenta Y por BALA_ALIEN_VELOCIDAD")
    void balaAbajoAumentaY() {
        Bala b = new Bala("b", 100, 200, Bala.Direccion.ABAJO, Bala.DUENIO_ALIEN);
        b.mover();
        assertEquals(200 + Config.BALA_ALIEN_VELOCIDAD, b.getY(), 1e-9);
    }

    @Test
    @DisplayName("esDelJugador() true si el dueño no es DUENIO_ALIEN")
    void esDelJugadorVerdadero() {
        Bala b = new Bala("b", 0, 0, Bala.Direccion.ARRIBA, "jugadorX");
        assertTrue(b.esDelJugador());
    }

    @Test
    @DisplayName("esDelJugador() false si el dueño es DUENIO_ALIEN")
    void esDelJugadorFalso() {
        Bala b = new Bala("b", 0, 0, Bala.Direccion.ABAJO, Bala.DUENIO_ALIEN);
        assertFalse(b.esDelJugador());
    }

    @Test
    @DisplayName("Dimensiones de la bala provienen de Config")
    void dimensionesDesdeConfig() {
        Bala b = new Bala("b", 0, 0, Bala.Direccion.ARRIBA, "x");
        assertEquals(Config.ANCHO_BALA, b.getAncho());
        assertEquals(Config.ALTO_BALA, b.getAlto());
    }
}
