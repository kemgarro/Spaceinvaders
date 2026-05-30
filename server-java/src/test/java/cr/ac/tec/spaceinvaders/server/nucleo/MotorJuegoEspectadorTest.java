package cr.ac.tec.spaceinvaders.server.nucleo;

import cr.ac.tec.spaceinvaders.server.util.Config;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas del soporte de espectadores con target en {@link MotorJuego}.
 *
 * <p>Cubren la nueva firma {@code registrarEspectador(idEspectador, idJugadorObservado)}
 * y la estructura de tracking {@code EstadoJuego.espectadoresPorJugador}.</p>
 */
class MotorJuegoEspectadorTest {

    @Test
    void registrarEspectadorConTargetNuloRetornaFalseYNoAgrega() {
        MotorJuego motor = new MotorJuego();
        motor.agregarJugador("p1", 400.0, Config.CANNON_Y);

        boolean resultado = motor.registrarEspectador("obs1", null);

        assertFalse(resultado, "target nulo debe rechazarse");
        EstadoJuego estado = motor.getEstadoInterno();
        assertFalse(estado.espectadores.contains("obs1"),
            "el espectador no debe quedar en el set");
        assertFalse(estado.espectadoresPorJugador.containsKey("obs1"),
            "no debe haber tracking del espectador");
    }

    @Test
    void registrarEspectadorConJugadorInexistenteRetornaFalse() {
        MotorJuego motor = new MotorJuego();
        motor.agregarJugador("p1", 400.0, Config.CANNON_Y);

        boolean resultado = motor.registrarEspectador("obs1", "p_inexistente");

        assertFalse(resultado, "target invalido debe rechazarse");
        EstadoJuego estado = motor.getEstadoInterno();
        assertFalse(estado.espectadores.contains("obs1"));
        assertFalse(estado.espectadoresPorJugador.containsKey("obs1"));
    }

    @Test
    void registrarEspectadorValidoAgregaAlSetYTrackeaLaRelacion() {
        MotorJuego motor = new MotorJuego();
        motor.agregarJugador("p1", 400.0, Config.CANNON_Y);

        boolean resultado = motor.registrarEspectador("obs1", "p1");

        assertTrue(resultado, "registro con target valido debe aceptarse");
        EstadoJuego estado = motor.getEstadoInterno();
        assertTrue(estado.espectadores.contains("obs1"),
            "el espectador debe quedar en el set");
        assertEquals("p1", estado.espectadoresPorJugador.get("obs1"),
            "el tracking debe asociar obs1 a p1");
    }

    @Test
    void registrarEspectadorIdempotenteNoDuplicaEnElSet() {
        MotorJuego motor = new MotorJuego();
        motor.agregarJugador("p1", 400.0, Config.CANNON_Y);
        motor.registrarEspectador("obs1", "p1");

        boolean segundo = motor.registrarEspectador("obs1", "p1");

        assertTrue(segundo, "el segundo registro idempotente debe retornar true");
        EstadoJuego estado = motor.getEstadoInterno();
        assertEquals(1, estado.espectadores.size(),
            "no debe haber duplicados en el set");
        assertEquals("p1", estado.espectadoresPorJugador.get("obs1"));
    }

    @Test
    void eliminarEspectadorSacaDelSetYDelTracking() {
        MotorJuego motor = new MotorJuego();
        motor.agregarJugador("p1", 400.0, Config.CANNON_Y);
        motor.registrarEspectador("obs1", "p1");

        motor.eliminarEspectador("obs1");

        EstadoJuego estado = motor.getEstadoInterno();
        assertFalse(estado.espectadores.contains("obs1"),
            "el espectador debe salir del set");
        assertFalse(estado.espectadoresPorJugador.containsKey("obs1"),
            "el tracking debe limpiarse");
    }

    @Test
    void cupoLlenoRechazaElSiguienteEspectador() {
        MotorJuego motor = new MotorJuego();
        motor.agregarJugador("p1", 400.0, Config.CANNON_Y);
        // Con 1 jugador activo el cupo es MAX_ESPECTADORES_POR_JUGADOR (2 por defecto).
        for (int i = 0; i < Config.MAX_ESPECTADORES_POR_JUGADOR; i++) {
            assertTrue(motor.registrarEspectador("obs" + i, "p1"),
                "espectador " + i + " deberia aceptarse dentro del cupo");
        }
        boolean extra = motor.registrarEspectador(
            "obs_extra_" + Config.MAX_ESPECTADORES_POR_JUGADOR, "p1");

        assertFalse(extra, "el espectador que excede el cupo debe rechazarse");
        assertEquals(Config.MAX_ESPECTADORES_POR_JUGADOR,
            motor.getEstadoInterno().espectadores.size());
    }

    @Test
    void getEstadoJuegoIncluyeEspectadoresPorJugador() {
        MotorJuego motor = new MotorJuego();
        motor.agregarJugador("p1", 400.0, Config.CANNON_Y);
        motor.registrarEspectador("obs1", "p1");

        Map<String, Object> snap = motor.getEstadoJuego();

        assertNotNull(snap);
        assertTrue(snap.containsKey("espectadoresPorJugador"),
            "el snapshot debe exponer la clave espectadoresPorJugador");
        Object valor = snap.get("espectadoresPorJugador");
        assertTrue(valor instanceof Map, "el valor debe ser un Map");
        @SuppressWarnings("unchecked")
        Map<String, String> mapeo = (Map<String, String>) valor;
        assertEquals("p1", mapeo.get("obs1"),
            "el snapshot debe reflejar el tracking obs1 -> p1");
    }
}
