package cr.ac.tec.spaceinvaders.admin;

import cr.ac.tec.spaceinvaders.server.util.Config;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Entry point del admin client GUI.
 *
 * <p>Lanza una {@link AdminVentana} con los valores por defecto del
 * proyecto ({@code Config.PUERTO_DEFAULT}, host {@code 127.0.0.1}, id
 * {@code admin}). Acepta argumentos opcionales:</p>
 *
 * <pre>
 *   java -cp build/libs/spaceinvaders-server-1.0.0.jar \
 *       cr.ac.tec.spaceinvaders.admin.AdminApp \
 *       [--host IP] [--port N] [--id ID]
 * </pre>
 *
 * <p>El admin no consume slots de jugador ni de espectador. Se conecta
 * vía socket TCP al mismo puerto que los demás clientes y envía mensajes
 * {@code ADMIN_CMD} con los comandos del enunciado (Crear, OVNI,
 * Velocidad, Bunkers).</p>
 */
public final class AdminApp {

    private AdminApp() {
    }

    public static void main(String[] args) {
        String host = "127.0.0.1";
        int puerto = Config.PUERTO_DEFAULT;
        String adminId = "admin";

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--host" -> { if (i + 1 < args.length) host = args[++i]; }
                case "--port" -> { if (i + 1 < args.length) puerto = parsearPuerto(args[++i], puerto); }
                case "--id"   -> { if (i + 1 < args.length) adminId = args[++i]; }
                case "--help", "-h" -> {
                    System.out.println("uso: AdminApp [--host IP] [--port N] [--id ID]");
                    return;
                }
                default -> System.err.println("argumento desconocido (ignorado): " + args[i]);
            }
        }

        final String hostFinal = host;
        final int puertoFinal = puerto;
        final String idFinal = adminId;

        // Tema look-and-feel del sistema cuando esta disponible (opcional, mejora la UX).
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // Si falla, Swing usa el L&F por defecto (Metal). No es bloqueante.
        }

        SwingUtilities.invokeLater(() -> {
            AdminVentana ventana = new AdminVentana(hostFinal, puertoFinal, idFinal);
            ventana.mostrar();
        });
    }

    private static int parsearPuerto(String texto, int fallback) {
        try {
            return Integer.parseInt(texto);
        } catch (NumberFormatException ex) {
            System.err.println("puerto invalido: '" + texto + "', usando " + fallback);
            return fallback;
        }
    }
}
