package cr.ac.tec.spaceinvaders.server.nucleo;

import cr.ac.tec.spaceinvaders.server.util.Config;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas de la API publica de {@link MotorJuego} (antes GameManager).
 */
class MotorJuegoApiTest {

    @Test
    void agregarJugadorAumentaConteoDeActivos() {
        MotorJuego gm = new MotorJuego();
        assertTrue(gm.agregarJugador("p1", Config.CAMPO_ANCHO / 2.0, Config.CANNON_Y));
        assertEquals(1, gm.contarJugadoresActivos());
        assertTrue(gm.hayJugadorActivo());
    }

    @Test
    void agregarJugadorRechazaCuandoSeAlcanzaMaximo() {
        MotorJuego gm = new MotorJuego();
        for (int i = 0; i < Config.MAX_JUGADORES; i++) {
            assertTrue(gm.agregarJugador("p" + i, 100.0 + i * 20, Config.CANNON_Y));
        }
        assertFalse(gm.agregarJugador("extra", 200.0, Config.CANNON_Y));
        assertEquals(Config.MAX_JUGADORES, gm.contarJugadoresActivos());
    }

    @Test
    void tieneEspacioSeVuelveFalseCuandoSeLlena() {
        MotorJuego gm = new MotorJuego();
        int totalMax = Config.MAX_JUGADORES
            + Config.MAX_JUGADORES * Config.MAX_ESPECTADORES_POR_JUGADOR;
        for (int i = 0; i < Config.MAX_JUGADORES; i++) {
            gm.agregarJugador("p" + i, 100.0 + i * 20, Config.CANNON_Y);
        }
        int espectadoresAAgregar = totalMax - Config.MAX_JUGADORES;
        for (int i = 0; i < espectadoresAAgregar; i++) {
            assertTrue(gm.registrarEspectador("e" + i));
        }
        assertFalse(gm.tieneEspacio());
    }

    @Test
    void procesarInputMueveCanonDelJugadorCorrecto() {
        MotorJuego gm = new MotorJuego();
        gm.agregarJugador("p1", 400.0, Config.CANNON_Y);
        Map<String, Object> antes = gm.getEstadoJuego();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> canonesAntes =
            (List<Map<String, Object>>) antes.get("canones");
        double xAntes = ((Number) canonesAntes.get(0).get("x")).doubleValue();

        gm.procesarInput("p1", "MOVE_LEFT");

        Map<String, Object> despues = gm.getEstadoJuego();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> canonesDespues =
            (List<Map<String, Object>>) despues.get("canones");
        double xDespues = ((Number) canonesDespues.get(0).get("x")).doubleValue();
        assertTrue(xDespues < xAntes, "el cañón debe haberse movido a la izquierda");
    }

    @Test
    void getEstadoJuegoExponeClavesEsperadas() {
        MotorJuego gm = new MotorJuego();
        Map<String, Object> snap = gm.getEstadoJuego();
        assertNotNull(snap);
        assertTrue(snap.containsKey("oleada"));
        assertTrue(snap.containsKey("aliens"));
        assertTrue(snap.containsKey("balas"));
        assertTrue(snap.containsKey("bunkers"));
        assertTrue(snap.containsKey("canones"));
        assertTrue(snap.containsKey("jugadores"));
        assertTrue(snap.containsKey("ovni"));
        assertTrue(snap.containsKey("juegoTerminado"));
        assertTrue(snap.containsKey("intervaloAliensMs"));
    }
}
