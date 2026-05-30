package cr.ac.tec.spaceinvaders.server.red;

import cr.ac.tec.spaceinvaders.server.nucleo.MotorJuego;
import cr.ac.tec.spaceinvaders.server.util.Config;
import cr.ac.tec.spaceinvaders.server.util.LoggerUtil;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Servidor TCP multicliente para el juego spaCEinvaders.
 * Acepta conexiones de jugadores y espectadores, manejando cada una en un hilo separado.
 */
public class ServidorJuego {
    private ServerSocket serverSocket;
    private boolean enEjecucion = true;
    private MotorJuego motor;
    private ExecutorService executorService;
    private int maxConexiones;
    
    /**
     * Constructor del servidor.
     */
    public ServidorJuego(int puerto, MotorJuego motor) {
        this.motor = motor;
        this.maxConexiones = Config.MAX_JUGADORES * (1 + Config.MAX_ESPECTADORES_POR_JUGADOR);
        this.executorService = Executors.newFixedThreadPool(maxConexiones);
        
        try {
            serverSocket = new ServerSocket(puerto);
            LoggerUtil.info("servidor iniciado en puerto " + puerto);
            LoggerUtil.info("esperando jugadores...");
        } catch (IOException e) {
            LoggerUtil.error("error al iniciar el servidor: " + e.getMessage());
        }
    }
    
    /**
     * Inicia el servidor y comienza a aceptar conexiones.
     */
    public void iniciar() {
        try {
            while (enEjecucion) {
                Socket cliente = serverSocket.accept();
                LoggerUtil.info("cliente conectado desde " + cliente.getInetAddress().getHostAddress());

                // Validación temprana de espacio disponible
                if (!motor.tieneEspacio()) {
                    LoggerUtil.warning("servidor lleno - rechazando conexión");
                    enviarMensajeRechazo(cliente, "Servidor lleno: máximo " + Config.MAX_JUGADORES + " jugadores con " + Config.MAX_ESPECTADORES_POR_JUGADOR + " espectadores cada uno");
                    cliente.close();
                    continue;
                }

                ManejadorCliente manejador = new ManejadorCliente(cliente, motor);
                executorService.submit(manejador);
            }
        } catch (IOException e) {
            if (enEjecucion) {
                LoggerUtil.error("error al aceptar conexiones: " + e.getMessage());
            }
        }
    }

    /**
     * Envía un mensaje de rechazo al cliente y cierra la conexión.
     */
    private void enviarMensajeRechazo(Socket cliente, String mensaje) {
        try {
            PrintWriter salida = new PrintWriter(cliente.getOutputStream(), true);
            String json = "{\"type\":\"ERROR\",\"message\":\"" + mensaje + "\"}";
            salida.println(json);
            salida.flush();
        } catch (IOException e) {
            LoggerUtil.error("error al enviar mensaje de rechazo: " + e.getMessage());
        }
    }
    
    /**
     * Detiene el servidor.
     */
    public void detener() {
        enEjecucion = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            executorService.shutdown();
            LoggerUtil.info("servidor detenido");
        } catch (IOException e) {
            LoggerUtil.error("error al cerrar el servidor: " + e.getMessage());
        }
    }
}
