package cr.ac.tec.spaceinvaders.server.nucleo;

import cr.ac.tec.spaceinvaders.server.entidades.Alien;
import cr.ac.tec.spaceinvaders.server.entidades.Bala;
import cr.ac.tec.spaceinvaders.server.entidades.Canon;
import cr.ac.tec.spaceinvaders.server.eventos.EventoJuego;
import cr.ac.tec.spaceinvaders.server.util.Config;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas internas del ciclo de tick de {@link MotorJuego}.
 *
 * <p>Usa los accesores package-private {@link MotorJuego#getEstadoInterno()},
 * {@link MotorJuego#tickInterno(DetectorColisiones.SinkEventos)} y
 * {@link MotorJuego#procesarAccionInterno(String, String)} para evitar el
 * lock global y manipular el estado directamente.</p>
 */
class MotorJuegoTest {

    /** Sink no-op para los tests que no inspeccionan eventos. */
    private DetectorColisiones.SinkEventos noopSink() {
        return e -> { };
    }

    /**
     * Prepara un MotorJuego con un jugador llamado "p1" agregado mediante
     * la API publica. La posicion del canon resulta de {@code agregarJugador}.
     */
    private MotorJuego prepararConJugador() {
        MotorJuego motor = new MotorJuego();
        motor.agregarJugador("p1", 400.0, Config.CANNON_Y);
        return motor;
    }

    @Test
    void moveLeftDesplazaCanonALaIzquierda() {
        MotorJuego motor = prepararConJugador();
        EstadoJuego estado = motor.getEstadoInterno();
        Canon canon = estado.canones.obtener(0);
        double xAntes = canon.getX();

        motor.procesarAccionInterno("p1", "MOVE_LEFT");

        assertEquals(xAntes - Config.CANNON_VELOCIDAD, canon.getX(), 1e-9);
    }

    @Test
    void fireRespetaMaxBalasJugador() {
        MotorJuego motor = prepararConJugador();
        EstadoJuego estado = motor.getEstadoInterno();

        motor.procesarAccionInterno("p1", "FIRE");
        int trasPrimero = estado.balas.tamano();
        motor.procesarAccionInterno("p1", "FIRE");
        int trasSegundo = estado.balas.tamano();

        assertEquals(1, trasPrimero, "el primer FIRE crea una bala");
        assertEquals(trasPrimero, trasSegundo,
            "no se permite una segunda bala mientras la primera vive");
    }

    @Test
    void aliensEnBordeDerechoInviertenYBajan() {
        /* Requiere al menos un jugador conectado: desde el fix de "motor
         * en pausa sin jugadores", el tick es no-op si jugadores.estaVacia(). */
        MotorJuego motor = prepararConJugador();
        EstadoJuego estado = motor.getEstadoInterno();

        // Mover manualmente todos los aliens hasta el borde derecho.
        for (Alien a : estado.aliens) {
            double xObjetivo = Config.CAMPO_ANCHO - a.getAncho();
            a.mover(); // captura la velocidad actual antes
            while (a.getX() < xObjetivo) {
                a.mover();
            }
        }
        // Guardar Y antes del rebote
        double yAntes = estado.aliens.obtener(0).getY();
        double velAntes = estado.aliens.obtener(0).getVelocidadX();

        motor.tickInterno(noopSink());

        Alien primero = estado.aliens.obtener(0);
        assertTrue(primero.getY() > yAntes, "los aliens deben haber bajado fila");
        assertTrue(primero.getVelocidadX() * velAntes < 0,
            "la velocidad horizontal debe haber cambiado de signo");
    }

    @Test
    void balaSaleDelCampoPorArribaYEsEliminada() {
        MotorJuego motor = prepararConJugador();
        EstadoJuego estado = motor.getEstadoInterno();
        Bala bala = new Bala("B_1", 400.0, -50.0, Bala.Direccion.ARRIBA, "p1");
        estado.balas.agregar(bala);

        motor.tickInterno(noopSink());

        assertEquals(0, estado.balas.tamano(),
            "la bala fuera del campo debe haberse eliminado");
        assertFalse(bala.estaVivo());
    }

    @Test
    void tickEmiteWaveClearedCuandoLosAliensSeAgotan() {
        /* Requiere al menos un jugador conectado: desde el fix de "motor
         * en pausa sin jugadores", el tick es no-op si jugadores.estaVacia(). */
        MotorJuego motor = prepararConJugador();
        EstadoJuego estado = motor.getEstadoInterno();
        estado.aliens.vaciar();

        java.util.List<EventoJuego> eventos = new java.util.ArrayList<>();
        motor.tickInterno(eventos::add);

        assertTrue(eventos.stream().anyMatch(
            e -> e.getTipo() == EventoJuego.TipoEvento.WAVE_CLEARED));
    }
}
