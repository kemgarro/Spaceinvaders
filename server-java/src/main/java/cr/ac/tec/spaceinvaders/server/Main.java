package cr.ac.tec.spaceinvaders.server;

import cr.ac.tec.spaceinvaders.server.cli.ConsolaAdmin;
import cr.ac.tec.spaceinvaders.server.nucleo.GameLoop;
import cr.ac.tec.spaceinvaders.server.nucleo.MotorJuego;
import cr.ac.tec.spaceinvaders.server.red.ServidorJuego;
import cr.ac.tec.spaceinvaders.server.util.Config;
import cr.ac.tec.spaceinvaders.server.util.LoggerUtil;

/**
 * Punto de entrada del servidor spaceinvaders.
 *
 * <p>Orquesta los cuatro componentes principales:</p>
 * <ol>
 *   <li>{@link MotorJuego} — estado y reglas del juego.</li>
 *   <li>{@link GameLoop} — tick periodico que avanza el motor.</li>
 *   <li>{@link ServidorJuego} — aceptador TCP multicliente.</li>
 *   <li>{@link ConsolaAdmin} — CLI administrativa por stdin.</li>
 * </ol>
 *
 * <p><strong>Orden de arranque:</strong> motor → loop → servidor (en su hilo
 * dedicado) → consola (en un hilo daemon). El proceso permanece vivo
 * bloqueado en {@code hiloServidor.join()}.</p>
 *
 * <p><strong>Shutdown hook</strong> detiene los componentes en orden inverso:
 * servidor → loop → consola → motor.</p>
 */
public class Main {

    private static MotorJuego motor;
    private static GameLoop loop;
    private static ServidorJuego servidor;
    private static ConsolaAdmin consola;
    private static Thread hiloServidor;
    private static Thread hiloConsola;

    public static void main(String[] args) {
        LoggerUtil.info("=== servidor spaceinvaders ===");

        motor = new MotorJuego();
        LoggerUtil.info("motor de juego inicializado");

        loop = new GameLoop(motor);

        int puerto = args.length > 0 ? Integer.parseInt(args[0]) : Config.PUERTO_DEFAULT;
        servidor = new ServidorJuego(puerto, motor);

        consola = new ConsolaAdmin(motor);

        // 1) Arrancar el loop (tick periodico del motor).
        loop.iniciar();

        // 2) Arrancar el servidor TCP en su propio hilo.
        hiloServidor = new Thread(servidor::iniciar, "servidor-tcp");
        hiloServidor.start();

        // 3) Arrancar la consola admin en un hilo daemon (asi no impide la
        //    salida del proceso si todo lo demas ya cerro).
        hiloConsola = new Thread(consola, "consola-admin");
        hiloConsola.setDaemon(true);
        hiloConsola.start();

        // Shutdown hook: detener en orden inverso al arranque.
        Runtime.getRuntime().addShutdownHook(new Thread(Main::cerrarTodo, "shutdown"));

        LoggerUtil.info("servidor listo en puerto " + puerto + " — escribi 'ayuda' para comandos admin");

        try {
            hiloServidor.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Detiene todos los componentes en orden inverso al arranque.
     * Es invocado por el shutdown hook registrado en {@link #main(String[])}.
     */
    private static void cerrarTodo() {
        LoggerUtil.info("cerrando servidor...");
        try {
            if (servidor != null) servidor.detener();
        } catch (Exception ex) {
            LoggerUtil.error("error al detener servidor: " + ex.getMessage());
        }
        try {
            if (loop != null) loop.detener();
        } catch (Exception ex) {
            LoggerUtil.error("error al detener gameloop: " + ex.getMessage());
        }
        try {
            if (consola != null) consola.detener();
        } catch (Exception ex) {
            LoggerUtil.error("error al detener consola admin: " + ex.getMessage());
        }
        try {
            if (motor != null) motor.shutdown();
        } catch (Exception ex) {
            LoggerUtil.error("error al detener motor: " + ex.getMessage());
        }
        LoggerUtil.info("servidor cerrado");
    }
}
