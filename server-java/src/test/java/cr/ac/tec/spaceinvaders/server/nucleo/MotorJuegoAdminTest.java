package cr.ac.tec.spaceinvaders.server.nucleo;

import cr.ac.tec.spaceinvaders.server.entidades.Bunker;
import cr.ac.tec.spaceinvaders.server.entidades.Ovni;
import cr.ac.tec.spaceinvaders.server.util.Config;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas de los metodos administrativos de {@link MotorJuego}
 * (comandos {@code Crear}, {@code OVNI}, {@code Velocidad}, {@code Bunkers}).
 */
class MotorJuegoAdminTest {

    // ============================================================
    // crearAlienAdmin
    // ============================================================

    @Test
    void crearAlienAdminAgregaAlienConPuntosValidos() {
        MotorJuego motor = new MotorJuego();
        EstadoJuego estado = motor.getEstadoInterno();
        int previo = estado.aliens.tamano();

        motor.crearAlienAdmin(123.0, 200.0, Config.PUNTOS_SQUID);

        assertEquals(previo + 1, estado.aliens.tamano(),
            "se debe haber agregado exactamente un alien");
    }

    @Test
    void crearAlienAdminConPuntosInvalidosNoCrasheaNiAgrega() {
        MotorJuego motor = new MotorJuego();
        EstadoJuego estado = motor.getEstadoInterno();
        int previo = estado.aliens.tamano();

        assertDoesNotThrow(() -> motor.crearAlienAdmin(50.0, 60.0, 999));
        assertEquals(previo, estado.aliens.tamano(),
            "no se debe agregar ningun alien con puntos invalidos");
    }

    // ============================================================
    // crearOvniAdmin
    // ============================================================

    @Test
    void crearOvniAdminFijaEstadoOvniConDireccionYPuntos() {
        MotorJuego motor = new MotorJuego();
        EstadoJuego estado = motor.getEstadoInterno();
        // Aseguramos que no haya OVNI vivo previo (en motor recien creado no hay).
        estado.ovni = null;

        motor.crearOvniAdmin(Ovni.Direccion.IZQUIERDA_A_DERECHA, 2000);

        assertNotNull(estado.ovni, "el OVNI debe haber sido creado");
        assertEquals(Ovni.Direccion.IZQUIERDA_A_DERECHA, estado.ovni.getDireccion());
        assertEquals(2000, estado.ovni.getPuntosBase());
    }

    @Test
    void crearOvniAdminNoSobreescribeSiYaHayOvniVivo() {
        MotorJuego motor = new MotorJuego();
        EstadoJuego estado = motor.getEstadoInterno();
        estado.ovni = null;

        motor.crearOvniAdmin(Ovni.Direccion.IZQUIERDA_A_DERECHA, 1500);
        Ovni primero = estado.ovni;
        assertNotNull(primero);

        // Segundo intento: debe ser ignorado y mantener el primero.
        motor.crearOvniAdmin(Ovni.Direccion.DERECHA_A_IZQUIERDA, 9999);

        assertSame(primero, estado.ovni,
            "el OVNI no debe haber cambiado de instancia (el segundo intento se ignora)");
        assertEquals(1500, estado.ovni.getPuntosBase());
        assertEquals(Ovni.Direccion.IZQUIERDA_A_DERECHA, estado.ovni.getDireccion());
    }

    // ============================================================
    // setVelocidadAliens
    // ============================================================

    @Test
    void setVelocidadAliensCambiaIntervalo() {
        MotorJuego motor = new MotorJuego();
        motor.setVelocidadAliens(100);
        assertEquals(100, motor.getEstadoInterno().intervaloAliensMs);
    }

    @Test
    void setVelocidadAliensClampaAlMinimo() {
        MotorJuego motor = new MotorJuego();
        motor.setVelocidadAliens(10);
        assertEquals(Config.ALIENS_INTERVALO_MIN_MS,
            motor.getEstadoInterno().intervaloAliensMs,
            "el intervalo se debe clampar al minimo");
    }

    @Test
    void setVelocidadAliensExponeIntervaloEnSnapshot() {
        MotorJuego motor = new MotorJuego();
        motor.setVelocidadAliens(150);
        Map<String, Object> snap = motor.getEstadoJuego();
        assertEquals(150L, ((Number) snap.get("intervaloAliensMs")).longValue());
    }

    // ============================================================
    // setSaludBunkers
    // ============================================================

    @Test
    void setSaludBunkers70DejaBunkersAl70() {
        MotorJuego motor = new MotorJuego();
        motor.setSaludBunkers(70);
        for (Bunker b : motor.getEstadoInterno().bunkers) {
            assertEquals(70, b.getSalud());
            assertEquals(70, b.getEstadoVisible());
            assertTrue(b.estaVivo(), "bunker al 70% sigue vivo");
        }
    }

    @Test
    void setSaludBunkers0DestruyeTodosLosBunkers() {
        MotorJuego motor = new MotorJuego();
        motor.setSaludBunkers(0);
        for (Bunker b : motor.getEstadoInterno().bunkers) {
            assertEquals(0, b.getSalud());
            assertFalse(b.estaVivo(), "bunker con salud 0 debe estar destruido");
        }
    }

    @Test
    void setSaludBunkersClampaNegativosACero() {
        MotorJuego motor = new MotorJuego();
        motor.setSaludBunkers(-10);
        for (Bunker b : motor.getEstadoInterno().bunkers) {
            assertEquals(0, b.getSalud());
        }
    }

    @Test
    void setSaludBunkersClampaMayoresACien() {
        MotorJuego motor = new MotorJuego();
        motor.setSaludBunkers(200);
        for (Bunker b : motor.getEstadoInterno().bunkers) {
            assertEquals(100, b.getSalud());
            assertTrue(b.estaVivo());
        }
    }

    @Test
    void setSaludBunkersReviveBunkerDestruido() {
        MotorJuego motor = new MotorJuego();
        // Primero destruimos.
        motor.setSaludBunkers(0);
        for (Bunker b : motor.getEstadoInterno().bunkers) {
            assertFalse(b.estaVivo());
        }
        // Luego revivimos al 50%.
        motor.setSaludBunkers(50);
        for (Bunker b : motor.getEstadoInterno().bunkers) {
            assertEquals(50, b.getSalud());
            assertTrue(b.estaVivo(), "bunker revivido al 50% debe estar vivo de nuevo");
        }
    }

    @Test
    void setSaludBunkersReflejaEnSnapshot() {
        MotorJuego motor = new MotorJuego();
        motor.setSaludBunkers(70);
        Map<String, Object> snap = motor.getEstadoJuego();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> bunkers = (List<Map<String, Object>>) snap.get("bunkers");
        assertFalse(bunkers.isEmpty(), "debe haber bunkers en el snapshot");
        for (Map<String, Object> bm : bunkers) {
            assertEquals(70, ((Number) bm.get("salud")).intValue());
        }
    }
}
