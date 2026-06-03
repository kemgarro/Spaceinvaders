package cr.ac.tec.spaceinvaders.admin;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import cr.ac.tec.spaceinvaders.server.red.Mensaje;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Conexion TCP del admin client contra el servidor spaCEinvaders.
 *
 * <p>Encapsula el socket, el envio sincronizado de mensajes JSON delimitados
 * por linea y un hilo lector que entrega cada respuesta del servidor al
 * callback {@code alRecibir}. La capa GUI no toca sockets directamente; solo
 * llama {@link #conectar()}, {@link #enviarComando(String, Map)} y
 * {@link #cerrar()}.</p>
 *
 * <p>Reusa la clase {@link Mensaje} del paquete server para garantizar que
 * el formato JSON sea exactamente el que entiende {@code ManejadorCliente}
 * en el otro extremo.</p>
 */
public class AdminConexion {

    private final String host;
    private final int puerto;
    private final String adminId;
    private final Consumer<String> alRecibir;
    private final Consumer<String> alLog;

    private final Gson gson = new GsonBuilder().create();
    private Socket socket;
    private PrintWriter salida;
    private BufferedReader entrada;
    private Thread hiloLector;
    private volatile boolean conectado;

    /**
     * Construye una conexion lista para abrir.
     *
     * @param host     direccion del servidor.
     * @param puerto   puerto TCP del servidor.
     * @param adminId  identificador del admin (campo {@code id} del CONNECT).
     * @param alRecibir callback que recibe cada linea JSON del servidor.
     * @param alLog     callback de log (informativo, por ejemplo "conectado").
     */
    public AdminConexion(String host, int puerto, String adminId,
                          Consumer<String> alRecibir, Consumer<String> alLog) {
        this.host = host;
        this.puerto = puerto;
        this.adminId = adminId;
        this.alRecibir = alRecibir;
        this.alLog = alLog;
    }

    /**
     * Abre el socket, envia el handshake {@code CONNECT} con
     * {@code clientType=ADMIN} y arranca el hilo lector. Si la apertura falla
     * lanza {@link IOException}.
     */
    public void conectar() throws IOException {
        socket = new Socket(host, puerto);
        salida = new PrintWriter(socket.getOutputStream(), true);
        entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        conectado = true;

        // Handshake CONNECT con clientType=ADMIN.
        Mensaje hello = new Mensaje();
        hello.setType(Mensaje.TipoMensaje.CONNECT);
        hello.setId(adminId);
        hello.setClientType("ADMIN");
        salida.println(gson.toJson(hello));

        hiloLector = new Thread(this::loopLectura, "admin-lector");
        hiloLector.setDaemon(true);
        hiloLector.start();

        alLog.accept("conectado a " + host + ":" + puerto + " como admin id=" + adminId);
    }

    /**
     * Envia un mensaje {@code ADMIN_CMD} con el nombre y payload dados.
     *
     * @param nombre  comando, p. ej. {@code CREATE_ALIEN}.
     * @param payload mapa con los parametros del comando.
     */
    public void enviarComando(String nombre, Map<String, Object> payload) {
        if (!conectado || salida == null) {
            alLog.accept("no se puede enviar: desconectado");
            return;
        }
        Mensaje m = new Mensaje();
        m.setType(Mensaje.TipoMensaje.ADMIN_CMD);
        m.setId(adminId);
        m.setName(nombre);
        m.setPayload(payload != null ? payload : new LinkedHashMap<>());
        String json = gson.toJson(m);
        synchronized (this) {
            salida.println(json);
        }
        alLog.accept("→ " + nombre + " " + json);
    }

    /**
     * Cierra la conexion limpiamente. Idempotente.
     */
    public void cerrar() {
        if (!conectado) return;
        conectado = false;
        try {
            if (salida != null) {
                Mensaje bye = new Mensaje();
                bye.setType(Mensaje.TipoMensaje.DISCONNECT);
                bye.setId(adminId);
                salida.println(gson.toJson(bye));
            }
        } catch (Exception ignored) {
            // Continuamos el cierre aunque el DISCONNECT falle.
        }
        try {
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException ex) {
            alLog.accept("error cerrando socket: " + ex.getMessage());
        }
        alLog.accept("desconectado");
    }

    /** @return {@code true} si el socket esta abierto y el handshake mando. */
    public boolean estaConectado() {
        return conectado && socket != null && !socket.isClosed();
    }

    /**
     * Loop del hilo lector: cada linea recibida se pasa a {@code alRecibir}.
     * Termina cuando el servidor cierra o se llama a {@link #cerrar()}.
     */
    private void loopLectura() {
        try {
            String linea;
            while (conectado && (linea = entrada.readLine()) != null) {
                alRecibir.accept(linea);
            }
        } catch (IOException ex) {
            if (conectado) {
                alLog.accept("lector caido: " + ex.getMessage());
            }
        } finally {
            conectado = false;
            alLog.accept("conexion terminada");
        }
    }
}
