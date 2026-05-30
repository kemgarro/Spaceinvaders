package cr.ac.tec.spaceinvaders.server.entidades;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas para el comportamiento común de {@link EntidadJuego}: colisiones AABB,
 * igualdad por id y manejo de estado vivo/destruido.
 */
class EntidadJuegoTest {

    /** Doble concreto mínimo de EntidadJuego para los tests. */
    private static class EntidadFalsa extends EntidadJuego {
        EntidadFalsa(String id, double x, double y, int ancho, int alto) {
            super(id, x, y, ancho, alto);
        }
        @Override public void mover() { /* no-op */ }
    }

    @Test
    @DisplayName("AABB: dos cajas que se solapan colisionan")
    void colisionConSolapamientoSimple() {
        EntidadFalsa a = new EntidadFalsa("a", 0, 0, 10, 10);
        EntidadFalsa b = new EntidadFalsa("b", 5, 5, 10, 10);
        assertTrue(a.colisionaCon(b));
        assertTrue(b.colisionaCon(a));
    }

    @Test
    @DisplayName("AABB: cajas separadas no colisionan")
    void cajasSeparadasNoColisionan() {
        EntidadFalsa a = new EntidadFalsa("a", 0, 0, 10, 10);
        EntidadFalsa b = new EntidadFalsa("b", 50, 50, 10, 10);
        assertFalse(a.colisionaCon(b));
        assertFalse(b.colisionaCon(a));
    }

    @Test
    @DisplayName("AABB: bordes que apenas se tocan NO colisionan (estricto)")
    void bordesTocandoseNoColisionan() {
        EntidadFalsa a = new EntidadFalsa("a", 0, 0, 10, 10);
        // b empieza exactamente donde termina a en X
        EntidadFalsa b = new EntidadFalsa("b", 10, 0, 10, 10);
        assertFalse(a.colisionaCon(b));
        // c empieza exactamente donde termina a en Y
        EntidadFalsa c = new EntidadFalsa("c", 0, 10, 10, 10);
        assertFalse(a.colisionaCon(c));
    }

    @Test
    @DisplayName("equals/hashCode están basados únicamente en el id")
    void equalsBasadoEnId() {
        EntidadFalsa a = new EntidadFalsa("mismo", 0, 0, 10, 10);
        EntidadFalsa b = new EntidadFalsa("mismo", 99, 99, 50, 50);
        EntidadFalsa c = new EntidadFalsa("otro", 0, 0, 10, 10);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, c);
    }

    @Test
    @DisplayName("destruir() pone estaVivo() en false")
    void destruirCambiaEstado() {
        EntidadFalsa a = new EntidadFalsa("a", 0, 0, 10, 10);
        assertTrue(a.estaVivo());
        a.destruir();
        assertFalse(a.estaVivo());
    }
}
