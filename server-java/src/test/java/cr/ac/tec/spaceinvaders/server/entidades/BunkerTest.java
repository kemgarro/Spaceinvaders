package cr.ac.tec.spaceinvaders.server.entidades;

import cr.ac.tec.spaceinvaders.server.util.Config;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas para {@link Bunker}: estado visible por buckets, impactos y comando admin.
 */
class BunkerTest {

    @Test
    @DisplayName("Salud inicial = BUNKER_SALUD_INTACTO")
    void saludInicialIntacto() {
        Bunker b = new Bunker("b", 0, 0);
        assertEquals(Config.BUNKER_SALUD_INTACTO, b.getSalud());
        assertEquals(Config.BUNKER_SALUD_INTACTO, b.getEstadoVisible());
        assertTrue(b.estaVivo());
    }

    @Test
    @DisplayName("getEstadoVisible: bucket 100 cuando salud == 100")
    void bucket100Exacto() {
        Bunker b = new Bunker("b", 0, 0);
        assertEquals(100, b.getEstadoVisible());
    }

    @Test
    @DisplayName("getEstadoVisible: bucket 70 entre 70 y 99")
    void bucket70() {
        Bunker b = new Bunker("b", 0, 0);
        b.fijarSaludAdmin(99);
        assertEquals(70, b.getEstadoVisible());
        b.fijarSaludAdmin(70);
        assertEquals(70, b.getEstadoVisible());
    }

    @Test
    @DisplayName("getEstadoVisible: bucket 40 entre 40 y 69")
    void bucket40() {
        Bunker b = new Bunker("b", 0, 0);
        b.fijarSaludAdmin(69);
        assertEquals(40, b.getEstadoVisible());
        b.fijarSaludAdmin(40);
        assertEquals(40, b.getEstadoVisible());
    }

    @Test
    @DisplayName("getEstadoVisible: bucket 0 para salud < 40")
    void bucket0() {
        Bunker b = new Bunker("b", 0, 0);
        b.fijarSaludAdmin(39);
        assertEquals(0, b.getEstadoVisible());
        b.fijarSaludAdmin(10);
        assertEquals(0, b.getEstadoVisible());
        b.fijarSaludAdmin(0);
        assertEquals(0, b.getEstadoVisible());
    }

    @Test
    @DisplayName("10 impactos consecutivos destruyen el bunker")
    void diezImpactosDestruyen() {
        Bunker b = new Bunker("b", 0, 0);
        for (int i = 0; i < 10; i++) {
            b.recibirImpacto();
        }
        assertFalse(b.estaVivo());
        assertEquals(Config.BUNKER_SALUD_DESTRUIDO, b.getSalud());
        assertEquals(0, b.getEstadoVisible());
    }

    @Test
    @DisplayName("fijarSaludAdmin(70) deja getEstadoVisible en 70")
    void adminFijaSalud70() {
        Bunker b = new Bunker("b", 0, 0);
        // gastar primero
        for (int i = 0; i < 5; i++) {
            b.recibirImpacto();
        }
        b.fijarSaludAdmin(70);
        assertEquals(70, b.getEstadoVisible());
        assertTrue(b.estaVivo());
    }

    @Test
    @DisplayName("fijarSaludAdmin(0) destruye; fijarSaludAdmin(100) resucita")
    void adminDestruyeYResucita() {
        Bunker b = new Bunker("b", 0, 0);
        b.fijarSaludAdmin(0);
        assertFalse(b.estaVivo());
        b.fijarSaludAdmin(100);
        assertTrue(b.estaVivo());
        assertEquals(100, b.getEstadoVisible());
    }

    @Test
    @DisplayName("fijarSaludAdmin clampea valores fuera de rango")
    void adminClampea() {
        Bunker b = new Bunker("b", 0, 0);
        b.fijarSaludAdmin(500);
        assertEquals(100, b.getSalud());
        b.fijarSaludAdmin(-50);
        assertEquals(0, b.getSalud());
    }
}
