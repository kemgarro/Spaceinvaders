package cr.ac.tec.spaceinvaders.server.entidades;

import cr.ac.tec.spaceinvaders.server.util.Config;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Pruebas para {@link Canon}: límites del campo y disparo.
 */
class CanonTest {

    @Test
    @DisplayName("moverIzquierda no baja de x=0")
    void moverIzquierdaClampea() {
        Canon c = new Canon("c", "j1", 2, Config.CANNON_Y);
        c.moverIzquierda(); // 2 - 5 = -3 → clamp a 0
        assertEquals(0.0, c.getX(), 1e-9);
        c.moverIzquierda(); // sigue en 0
        assertEquals(0.0, c.getX(), 1e-9);
    }

    @Test
    @DisplayName("moverDerecha no excede CAMPO_ANCHO - ancho")
    void moverDerechaClampea() {
        double xCerca = Config.CAMPO_ANCHO - Config.ANCHO_CANNON - 2;
        Canon c = new Canon("c", "j1", xCerca, Config.CANNON_Y);
        c.moverDerecha(); // se pasaría → clamp
        assertEquals(Config.CAMPO_ANCHO - Config.ANCHO_CANNON, c.getX(), 1e-9);
        c.moverDerecha(); // sigue en el tope
        assertEquals(Config.CAMPO_ANCHO - Config.ANCHO_CANNON, c.getX(), 1e-9);
    }

    @Test
    @DisplayName("moverIzquierda dentro del campo resta CANNON_VELOCIDAD")
    void moverIzquierdaNormal() {
        Canon c = new Canon("c", "j1", 100, Config.CANNON_Y);
        c.moverIzquierda();
        assertEquals(100 - Config.CANNON_VELOCIDAD, c.getX(), 1e-9);
    }

    @Test
    @DisplayName("moverDerecha dentro del campo suma CANNON_VELOCIDAD")
    void moverDerechaNormal() {
        Canon c = new Canon("c", "j1", 100, Config.CANNON_Y);
        c.moverDerecha();
        assertEquals(100 + Config.CANNON_VELOCIDAD, c.getX(), 1e-9);
    }

    @Test
    @DisplayName("disparar genera bala ARRIBA centrada arriba del cañón, dueño=jugadorId")
    void dispararGeneraBalaCorrecta() {
        double xCanon = 200;
        double yCanon = Config.CANNON_Y;
        Canon c = new Canon("c", "jugadorA", xCanon, yCanon);

        Bala b = c.disparar("bala-1");

        assertEquals(Bala.Direccion.ARRIBA, b.getDireccion());
        assertEquals("jugadorA", b.getDuenioId());
        // Centrada horizontalmente
        double xEsperado = xCanon + (Config.ANCHO_CANNON - Config.ANCHO_BALA) / 2.0;
        assertEquals(xEsperado, b.getX(), 1e-9);
        // Justo arriba del cañón
        assertEquals(yCanon - Config.ALTO_BALA, b.getY(), 1e-9);
    }

    @Test
    @DisplayName("getJugadorId expone el id del jugador propietario")
    void getJugadorIdCorrecto() {
        Canon c = new Canon("c", "jugadorZ", 0, 0);
        assertEquals("jugadorZ", c.getJugadorId());
    }
}
