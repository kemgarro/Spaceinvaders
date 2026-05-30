package cr.ac.tec.spaceinvaders.server.cli;

import cr.ac.tec.spaceinvaders.server.eventos.EventoJuego;
import cr.ac.tec.spaceinvaders.server.nucleo.MotorJuego;
import cr.ac.tec.spaceinvaders.server.util.Config;

import java.util.List;
import java.util.Map;

/**
 * CLI de prueba (Fase 1.5) para ejercitar el motor sin red.
 *
 * <p>Crea un {@link MotorJuego}, suscribe un observador que imprime los
 * eventos relevantes, agrega dos jugadores y avanza ~200 ticks con inputs
 * simulados. Sirve como humo rápido para confirmar que oleadas, balas,
 * colisiones y bunkers viven y mueren como se espera.</p>
 */
public class PruebaConsola {

    public static void main(String[] args) throws InterruptedException {
        MotorJuego motor = new MotorJuego();
        System.out.println("== Prueba consola motor de juego ==");

        motor.agregarObservador(dato -> {
            if (dato instanceof EventoJuego e) {
                System.out.println("[EVENTO] " + e.getTipo() + " " + e.getPayload());
            }
            // Ignora snapshots de estado (mapa) para no saturar la salida.
        });

        motor.agregarJugador("p1", Config.CAMPO_ANCHO / 2.0, Config.CANNON_Y);
        motor.agregarJugador("p2", Config.CAMPO_ANCHO / 2.0 + 60, Config.CANNON_Y);
        System.out.println("Jugadores activos: " + motor.contarJugadoresActivos());

        for (int i = 0; i < 200; i++) {
            if (i % 25 == 0) motor.procesarInput("p1", "FIRE");
            if (i % 30 == 0) motor.procesarInput("p2", "FIRE");
            if (i % 5 == 0) motor.procesarInput("p1", "MOVE_RIGHT");
            if (i % 8 == 0) motor.procesarInput("p2", "MOVE_LEFT");

            motor.actualizar(Config.INTERVALO_TICK_MS / 1000.0);

            if (i % 20 == 0) {
                Map<String, Object> snap = motor.getEstadoJuego();
                int nAliens = ((List<?>) snap.get("aliens")).size();
                int nBalas = ((List<?>) snap.get("balas")).size();
                int nBunkers = ((List<?>) snap.get("bunkers")).size();
                System.out.printf(
                    "tick=%3d oleada=%s aliens=%d balas=%d bunkers=%d juegoTerminado=%s%n",
                    i, snap.get("oleada"), nAliens, nBalas, nBunkers, snap.get("juegoTerminado")
                );
            }
            Thread.sleep(1);
        }
        motor.shutdown();
        System.out.println("== Fin prueba ==");
    }
}
