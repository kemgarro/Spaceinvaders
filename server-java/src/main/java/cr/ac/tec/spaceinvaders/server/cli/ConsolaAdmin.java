package cr.ac.tec.spaceinvaders.server.cli;

import cr.ac.tec.spaceinvaders.server.entidades.Ovni;
import cr.ac.tec.spaceinvaders.server.nucleo.MotorJuego;
import cr.ac.tec.spaceinvaders.server.util.LoggerUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Interfaz de linea de comandos (CLI) para que el operador del servidor inyecte
 * eventos al juego en tiempo real. Es ejecutada por {@link Main} en un hilo
 * daemon llamado {@code "consola-admin"} y lee de {@link System#in} linea por
 * linea con un {@link BufferedReader}.
 *
 * <p><strong>Comandos soportados</strong> (formato exacto del enunciado):</p>
 * <ul>
 *   <li>{@code Crear (X, Y, Pts)} — Spawnea un alien adicional. Los espacios
 *       alrededor de las comas son opcionales. {@code Pts} debe ser
 *       {@code 10}, {@code 20} o {@code 40}.</li>
 *   <li>{@code OVNI I-D <pts>} / {@code OVNI D-I <pts>} — Spawnea un OVNI con
 *       la direccion indicada ({@code I-D} = izquierda a derecha,
 *       {@code D-I} = derecha a izquierda) y puntos base {@code pts}. Si ya
 *       hay un OVNI vivo, se rechaza.</li>
 *   <li>{@code Velocidad <ms>} — Cambia el intervalo (ms) entre pasos del
 *       bloque de aliens.</li>
 *   <li>{@code Bunkers <pct>%} — Fija la salud de todos los bunkers al
 *       porcentaje indicado. El simbolo {@code %} es opcional.</li>
 *   <li>{@code ayuda} / {@code help} — Imprime la lista de comandos.</li>
 *   <li>{@code salir} / {@code exit} / {@code quit} — Detiene la consola
 *       (no el servidor; solo este hilo).</li>
 * </ul>
 *
 * <p><strong>Reglas de robustez:</strong></p>
 * <ul>
 *   <li>Las lineas se trimean antes de parsear; las lineas vacias se ignoran.</li>
 *   <li>Las excepciones del motor ({@link IllegalArgumentException} y otras)
 *       se capturan y loggean; el thread nunca termina por un comando malo.</li>
 *   <li>Si la lectura de stdin recibe EOF ({@code readLine()} retorna
 *       {@code null}, ej. al cerrarse el pipe con {@code Ctrl+D}), la consola
 *       sale limpiamente.</li>
 *   <li>Si el hilo recibe una interrupcion durante la lectura, sale tambien
 *       de manera ordenada.</li>
 * </ul>
 */
public class ConsolaAdmin implements Runnable {

    /** Motor del juego al que se inyectan los comandos administrativos. */
    private final MotorJuego motor;

    /** Reader de stdin; expuesto como campo para poder cerrarlo desde {@link #detener()}. */
    private BufferedReader lector;

    /**
     * Flag de ejecucion. Solo es {@code volatile} porque {@link #detener()}
     * puede llamarse desde otro hilo (ej. el shutdown hook).
     */
    private volatile boolean ejecutando = false;

    // ============================================================
    // Patrones de parseo de los 4 comandos del enunciado
    // ============================================================

    private static final Pattern PATRON_CREAR =
        Pattern.compile("Crear\\s*\\(\\s*(\\d+)\\s*,\\s*(\\d+)\\s*,\\s*(\\d+)\\s*\\)");

    private static final Pattern PATRON_OVNI =
        Pattern.compile("OVNI\\s+(I-D|D-I)\\s+(\\d+)");

    private static final Pattern PATRON_VELOCIDAD =
        Pattern.compile("Velocidad\\s+(\\d+)");

    private static final Pattern PATRON_BUNKERS =
        Pattern.compile("Bunkers\\s+(\\d+)\\s*%?");

    /**
     * Construye la consola asociada al motor dado. No arranca la lectura
     * de stdin hasta que se invoque {@link #run()} (tipicamente desde un
     * hilo).
     *
     * @param motor motor del juego al que se enviaran los comandos admin.
     */
    public ConsolaAdmin(MotorJuego motor) {
        this.motor = motor;
    }

    /**
     * Loop principal: lee lineas de {@link System#in} hasta que llegue EOF,
     * se reciba el comando {@code salir} o se invoque {@link #detener()}.
     *
     * <p>Imprime un prompt {@code "admin> "} antes de cada lectura para que
     * el operador sepa que la consola esta lista. Cualquier excepcion al
     * procesar una linea se captura para que el thread sobreviva.</p>
     */
    @Override
    public void run() {
        ejecutando = true;
        lector = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
        LoggerUtil.info("consola admin lista (escribi 'ayuda' para ver comandos)");
        imprimirAyuda();
        while (ejecutando && !Thread.currentThread().isInterrupted()) {
            try {
                System.out.print("admin> ");
                System.out.flush();
                String linea = lector.readLine();
                if (linea == null) {
                    // EOF (ej. Ctrl+D o stdin cerrado): salimos limpiamente.
                    LoggerUtil.info("consola admin: EOF en stdin, cerrando");
                    break;
                }
                procesarLinea(linea);
            } catch (IOException ex) {
                if (ejecutando) {
                    LoggerUtil.error("consola admin: error de I/O: " + ex.getMessage());
                }
                break;
            } catch (Exception ex) {
                LoggerUtil.error("consola admin: excepcion procesando linea: " + ex.getMessage());
            }
        }
        ejecutando = false;
        LoggerUtil.info("consola admin terminada");
    }

    /**
     * Detiene la consola: setea la bandera de ejecucion en {@code false} y
     * cierra el {@link BufferedReader} para desbloquear cualquier
     * {@code readLine} pendiente.
     */
    public void detener() {
        ejecutando = false;
        try {
            if (lector != null) {
                lector.close();
            }
        } catch (IOException ex) {
            LoggerUtil.warning("consola admin: error al cerrar reader: " + ex.getMessage());
        }
    }

    // ============================================================
    // Logica de parseo (package-private para facilitar las pruebas)
    // ============================================================

    /**
     * Procesa una linea cruda de comando.
     *
     * <p>Tras hacer trim, intenta matchear los 4 patrones del enunciado o
     * los alias de ayuda/salida. Si nada matchea, loggea una advertencia y
     * muestra una pista de uso.</p>
     *
     * @param linea linea recibida de stdin (puede ser {@code null} o vacia).
     */
    void procesarLinea(String linea) {
        if (linea == null) return;
        String texto = linea.trim();
        if (texto.isEmpty()) return;

        // Ayuda
        if (texto.equalsIgnoreCase("ayuda") || texto.equalsIgnoreCase("help")) {
            imprimirAyuda();
            return;
        }

        // Salida (no detiene el servidor, solo el thread de la consola).
        if (texto.equalsIgnoreCase("salir")
            || texto.equalsIgnoreCase("exit")
            || texto.equalsIgnoreCase("quit")) {
            LoggerUtil.info("admin: comando salir recibido, deteniendo consola");
            ejecutando = false;
            return;
        }

        Matcher mCrear = PATRON_CREAR.matcher(texto);
        if (mCrear.matches()) {
            try {
                double x = Double.parseDouble(mCrear.group(1));
                double y = Double.parseDouble(mCrear.group(2));
                int pts = Integer.parseInt(mCrear.group(3));
                motor.crearAlienAdmin(x, y, pts);
            } catch (Exception ex) {
                LoggerUtil.warning("admin: error en Crear: " + ex.getMessage());
            }
            return;
        }

        Matcher mOvni = PATRON_OVNI.matcher(texto);
        if (mOvni.matches()) {
            try {
                String dirStr = mOvni.group(1);
                int pts = Integer.parseInt(mOvni.group(2));
                Ovni.Direccion dir = "I-D".equals(dirStr)
                    ? Ovni.Direccion.IZQUIERDA_A_DERECHA
                    : Ovni.Direccion.DERECHA_A_IZQUIERDA;
                motor.crearOvniAdmin(dir, pts);
            } catch (Exception ex) {
                LoggerUtil.warning("admin: error en OVNI: " + ex.getMessage());
            }
            return;
        }

        Matcher mVel = PATRON_VELOCIDAD.matcher(texto);
        if (mVel.matches()) {
            try {
                long ms = Long.parseLong(mVel.group(1));
                motor.setVelocidadAliens(ms);
            } catch (Exception ex) {
                LoggerUtil.warning("admin: error en Velocidad: " + ex.getMessage());
            }
            return;
        }

        Matcher mBunk = PATRON_BUNKERS.matcher(texto);
        if (mBunk.matches()) {
            try {
                int pct = Integer.parseInt(mBunk.group(1));
                motor.setSaludBunkers(pct);
            } catch (Exception ex) {
                LoggerUtil.warning("admin: error en Bunkers: " + ex.getMessage());
            }
            return;
        }

        LoggerUtil.warning("comando desconocido: " + texto);
        System.out.println("uso: Crear (X, Y, Pts) | OVNI I-D <pts> | OVNI D-I <pts> | Velocidad <ms> | Bunkers <pct>% | ayuda | salir");
    }

    /** Imprime la lista completa de comandos disponibles. */
    private void imprimirAyuda() {
        System.out.println("comandos disponibles:");
        System.out.println("  Crear (X, Y, Pts)    spawnea un alien (Pts = 10, 20 o 40)");
        System.out.println("  OVNI I-D <pts>       spawnea un OVNI de izquierda a derecha");
        System.out.println("  OVNI D-I <pts>       spawnea un OVNI de derecha a izquierda");
        System.out.println("  Velocidad <ms>       cambia el intervalo de movimiento de aliens");
        System.out.println("  Bunkers <pct>%       fija la salud de todos los bunkers");
        System.out.println("  ayuda | help         muestra esta ayuda");
        System.out.println("  salir | exit | quit  cierra la consola (no el servidor)");
    }
}
