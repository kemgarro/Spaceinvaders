package cr.ac.tec.spaceinvaders.server.entidades;

import cr.ac.tec.spaceinvaders.server.util.Config;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas para {@link Jugador}: vidas, puntaje y asignación de cañón.
 */
class JugadorTest {

    @Test
    @DisplayName("Constructor: puntaje=0, vidas=VIDAS_INICIALES, canon=null")
    void estadoInicial() {
        Jugador j = new Jugador("j1", "Ana");
        assertEquals(0, j.getPuntaje());
        assertEquals(Config.VIDAS_INICIALES, j.getVidas());
        assertEquals("j1", j.getId());
        assertEquals("Ana", j.getNombre());
        assertNull(j.getCanon());
        assertTrue(j.estaVivo());
    }

    @Test
    @DisplayName("perderVida reduce vidas hasta 0, no más")
    void perderVidaClampeaA0() {
        Jugador j = new Jugador("j1", "Ana");
        for (int i = 0; i < Config.VIDAS_INICIALES; i++) {
            j.perderVida();
        }
        assertEquals(0, j.getVidas());
        assertFalse(j.estaVivo());
        // intentar perder más no baja de 0
        j.perderVida();
        j.perderVida();
        assertEquals(0, j.getVidas());
    }

    @Test
    @DisplayName("ganarVida aumenta sin cap (hasta 10)")
    void ganarVidaSinCap() {
        Jugador j = new Jugador("j1", "Ana");
        int inicial = j.getVidas();
        for (int i = 0; i < 10; i++) {
            j.ganarVida();
        }
        assertEquals(inicial + 10, j.getVidas());
        assertTrue(j.estaVivo());
    }

    @Test
    @DisplayName("estaVivo true mientras vidas > 0")
    void estaVivoConVidas() {
        Jugador j = new Jugador("j1", "Ana");
        assertTrue(j.estaVivo());
        j.perderVida(); // 2
        assertTrue(j.estaVivo());
        j.perderVida(); // 1
        assertTrue(j.estaVivo());
        j.perderVida(); // 0
        assertFalse(j.estaVivo());
    }

    @Test
    @DisplayName("agregarPuntos suma correctamente con valores no negativos")
    void agregarPuntosPositivos() {
        Jugador j = new Jugador("j1", "Ana");
        j.agregarPuntos(10);
        j.agregarPuntos(40);
        j.agregarPuntos(0);
        assertEquals(50, j.getPuntaje());
    }

    @Test
    @DisplayName("agregarPuntos rechaza negativos con IllegalArgumentException")
    void agregarPuntosNegativosLanza() {
        Jugador j = new Jugador("j1", "Ana");
        assertThrows(IllegalArgumentException.class, () -> j.agregarPuntos(-1));
    }

    @Test
    @DisplayName("asignarCanon guarda la referencia recuperable por getCanon")
    void asignarCanon() {
        Jugador j = new Jugador("j1", "Ana");
        Canon c = new Canon("c1", "j1", 100, 550);
        j.asignarCanon(c);
        assertSame(c, j.getCanon());
    }
}
