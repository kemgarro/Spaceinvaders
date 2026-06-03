package cr.ac.tec.spaceinvaders.server.red;

import cr.ac.tec.spaceinvaders.server.entidades.Ovni;
import cr.ac.tec.spaceinvaders.server.nucleo.MotorJuego;
import cr.ac.tec.spaceinvaders.server.eventos.EventoJuego;
import cr.ac.tec.spaceinvaders.server.observador.GameObserver;
import cr.ac.tec.spaceinvaders.server.util.Config;
import cr.ac.tec.spaceinvaders.server.util.LoggerUtil;

import java.io.*;
import java.net.Socket;
import java.util.Map;

/**
 * Maneja la comunicación con un cliente individual.
 * Implementa GameObserver para recibir actualizaciones del motor de juego.
 */
public class ManejadorCliente implements Runnable, GameObserver {

    /**
     * Tipo de cliente conectado.
     */
    private enum TipoCliente {
        PLAYER,      // Jugador activo que puede enviar inputs
        SPECTATOR,   // Espectador que solo recibe estado
        ADMIN,       // Administrador remoto que envia ADMIN_CMD
        UNDEFINED    // No se ha determinado el tipo
    }

    private Socket socket;
    private MotorJuego motor;
    private BufferedReader entrada;
    private PrintWriter salida;
    private final Object salidaLock = new Object();
    private String jugadorId;
    private boolean conectado;
    private TipoCliente tipoCliente;
    
    /**
     * Constructor del manejador de cliente.
     */
    public ManejadorCliente(Socket socket, MotorJuego motor) {
        this.socket = socket;
        this.motor = motor;
        this.conectado = true;
        this.tipoCliente = TipoCliente.UNDEFINED;
    }
    
    @Override
    public void run() {
        try {
            entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            salida = new PrintWriter(socket.getOutputStream(), true);
            
            // Registrar como observador
            motor.agregarObservador(this);
            
            // Enviar estado inicial
            enviarEstado();
            
            // Leer mensajes del cliente
            String linea;
            while (conectado && (linea = entrada.readLine()) != null) {
                procesarMensaje(linea);
            }
        } catch (IOException e) {
            LoggerUtil.warning("cliente desconectado: " + e.getMessage());
        } finally {
            desconectar();
        }
    }
    
    /**
     * Procesa un mensaje JSON recibido del cliente.
     */
    private void procesarMensaje(String json) {
        try {
            Mensaje mensaje = JsonUtil.fromJson(json);
            if (mensaje == null) {
                LoggerUtil.warning("mensaje json invalido recibido");
                return;
            }
            
            switch (mensaje.getType()) {
                case CONNECT:
                    manejarConexion(mensaje);
                    break;
                case INPUT:
                    manejarInput(mensaje);
                    break;
                case ADMIN_CMD:
                    manejarAdminCmd(mensaje);
                    break;
                case DISCONNECT:
                    desconectar();
                    break;
                default:
                    LoggerUtil.debug("tipo de mensaje no reconocido: " + mensaje.getType());
            }
        } catch (Exception e) {
            LoggerUtil.error("error al procesar mensaje: " + e.getMessage());
            enviarError("error al procesar mensaje: " + e.getMessage());
        }
    }
    
    /**
     * Maneja la conexión de un nuevo cliente.
     * Determina si el cliente es jugador o espectador según el campo clientType.
     * Si no se especifica clientType, se asume "PLAYER" por compatibilidad.
     */
    private void manejarConexion(Mensaje mensaje) {
        if (mensaje.getId() == null) {
            enviarError("ID de cliente requerido");
            desconectar();
            return;
        }

        jugadorId = mensaje.getId();

        // Extraer tipo de cliente del mensaje (default: PLAYER)
        String clientTypeStr = mensaje.getClientType();
        if (clientTypeStr == null || clientTypeStr.isEmpty()) {
            clientTypeStr = "PLAYER";  // Compatibilidad con clientes antiguos
        }

        // Procesar según tipo de cliente
        if ("PLAYER".equalsIgnoreCase(clientTypeStr)) {
            manejarConexionJugador();
        } else if ("SPECTATOR".equalsIgnoreCase(clientTypeStr)) {
            manejarConexionEspectador(mensaje);
        } else if ("ADMIN".equalsIgnoreCase(clientTypeStr)) {
            manejarConexionAdmin();
        } else {
            enviarError("Tipo de cliente inválido: " + clientTypeStr);
            desconectar();
        }
    }

    /**
     * Procesa la conexion de un administrador remoto.
     *
     * <p>El admin no consume un slot de jugador ni de espectador: solo se
     * registra como observador para recibir {@code STATE}/{@code EVENT} y
     * poder mandar mensajes {@code ADMIN_CMD} contra el motor. No se crea
     * ningun canon ni se trackea relacion con jugadores.</p>
     */
    private void manejarConexionAdmin() {
        tipoCliente = TipoCliente.ADMIN;
        LoggerUtil.info("administrador " + jugadorId + " conectado");
        enviarEstado();
    }

    /**
     * Procesa la conexión de un jugador.
     */
    private void manejarConexionJugador() {
        boolean agregado = motor.agregarJugador(jugadorId, Config.CAMPO_ANCHO / 2.0, Config.CANNON_Y);

        if (agregado) {
            tipoCliente = TipoCliente.PLAYER;
            LoggerUtil.info("jugador " + jugadorId + " registrado exitosamente");
            enviarEstado();
        } else {
            enviarError("No se puede conectar como jugador: límite alcanzado (máximo " +
                       motor.contarJugadoresActivos() + " jugador)");
            LoggerUtil.warning("conexión de jugador " + jugadorId + " rechazada: límite alcanzado");
            desconectar();
        }
    }

    /**
     * Procesa la conexion de un espectador.
     *
     * <p>El protocolo exige que el espectador declare en el mismo mensaje
     * {@code CONNECT} cual jugador piensa observar mediante el campo
     * {@code target}. Si falta o apunta a un jugador inexistente se rechaza
     * la conexion con un {@code ERROR} y se cierra el socket.</p>
     *
     * @param mensaje mensaje CONNECT recibido del cliente.
     */
    private void manejarConexionEspectador(Mensaje mensaje) {
        // Verificar que haya al menos un jugador activo
        if (!motor.hayJugadorActivo()) {
            enviarError("No hay partidas activas para observar");
            LoggerUtil.warning("espectador " + jugadorId + " rechazado: no hay jugadores activos");
            desconectar();
            return;
        }

        // El espectador DEBE indicar a que jugador va a observar.
        String target = mensaje.getTarget();
        if (target == null || target.isEmpty()) {
            enviarError("Espectador requiere campo target (id del jugador a observar)");
            LoggerUtil.warning("espectador " + jugadorId + " rechazado: falta campo target");
            desconectar();
            return;
        }

        // Intentar registrar como espectador del jugador objetivo.
        boolean registrado = motor.registrarEspectador(jugadorId, target);

        if (registrado) {
            tipoCliente = TipoCliente.SPECTATOR;
            LoggerUtil.info("espectador " + jugadorId + " conectado exitosamente observando a " + target);
            enviarEstado();
        } else {
            enviarError("No se puede conectar como espectador: jugador target inexistente o cupo lleno: " + target);
            LoggerUtil.warning("conexion de espectador " + jugadorId
                + " rechazada: target invalido o cupo lleno (target=" + target + ")");
            desconectar();
        }
    }
    
    /**
     * Maneja el input de un jugador.
     * Los espectadores NO pueden enviar inputs.
     */
    private void manejarInput(Mensaje mensaje) {
        // Verificar que sea un jugador
        if (tipoCliente != TipoCliente.PLAYER) {
            enviarError("Los espectadores no pueden enviar inputs");
            LoggerUtil.warning("espectador " + jugadorId + " intentó enviar input (rechazado)");
            return;
        }

        if (jugadorId == null) {
            enviarError("Cliente no identificado");
            return;
        }

        String accion = mensaje.getAction();
        if (accion != null) {
            motor.procesarInput(jugadorId, accion);
        }
    }

    /**
     * Maneja un comando ADMIN_CMD enviado por un administrador remoto.
     *
     * <p>El campo {@code name} indica el comando ({@code CREATE_ALIEN},
     * {@code SPAWN_OVNI}, {@code SET_VELOCIDAD}, {@code SET_BUNKERS}) y el
     * {@code payload} es un mapa con los parametros propios de cada uno.
     * Reusa los metodos publicos del motor {@code crearAlienAdmin},
     * {@code crearOvniAdmin}, {@code setVelocidadAliens} y
     * {@code setSaludBunkers}.</p>
     *
     * <p>Si el cliente no se identifico como ADMIN, el comando se rechaza
     * con {@code ERROR}.</p>
     *
     * @param mensaje mensaje ADMIN_CMD recibido.
     */
    private void manejarAdminCmd(Mensaje mensaje) {
        if (tipoCliente != TipoCliente.ADMIN) {
            enviarError("Solo el cliente ADMIN puede enviar ADMIN_CMD");
            LoggerUtil.warning("cliente " + jugadorId
                + " envio ADMIN_CMD sin ser ADMIN (tipo=" + tipoCliente + ")");
            return;
        }

        String comando = mensaje.getName();
        if (comando == null || comando.isEmpty()) {
            enviarError("ADMIN_CMD requiere el campo 'name'");
            return;
        }

        Object rawPayload = mensaje.getPayload();
        @SuppressWarnings("unchecked")
        Map<String, Object> payload = (rawPayload instanceof Map)
            ? (Map<String, Object>) rawPayload
            : java.util.Collections.emptyMap();

        try {
            switch (comando.toUpperCase()) {
                case "CREATE_ALIEN" -> {
                    double x = leerNumero(payload, "x");
                    double y = leerNumero(payload, "y");
                    int pts = (int) leerNumero(payload, "pts");
                    motor.crearAlienAdmin(x, y, pts);
                }
                case "SPAWN_OVNI" -> {
                    String dirStr = String.valueOf(payload.getOrDefault("direccion", "I-D"));
                    int puntosBase = (int) leerNumero(payload, "puntosBase");
                    Ovni.Direccion dir = "I-D".equalsIgnoreCase(dirStr)
                        ? Ovni.Direccion.IZQUIERDA_A_DERECHA
                        : Ovni.Direccion.DERECHA_A_IZQUIERDA;
                    motor.crearOvniAdmin(dir, puntosBase);
                }
                case "SET_VELOCIDAD" -> {
                    long ms = (long) leerNumero(payload, "intervaloMs");
                    motor.setVelocidadAliens(ms);
                }
                case "SET_BUNKERS" -> {
                    int pct = (int) leerNumero(payload, "pct");
                    motor.setSaludBunkers(pct);
                }
                default -> {
                    enviarError("Comando ADMIN_CMD desconocido: " + comando);
                    LoggerUtil.warning("ADMIN_CMD desconocido: " + comando);
                }
            }
        } catch (Exception ex) {
            enviarError("Error ejecutando " + comando + ": " + ex.getMessage());
            LoggerUtil.warning("error en ADMIN_CMD " + comando + ": " + ex.getMessage());
        }
    }

    /**
     * Lee un valor numerico del payload con tolerancia: acepta {@link Number}
     * directo o {@link String} parseable. Devuelve 0 si la clave no existe.
     */
    private double leerNumero(Map<String, Object> payload, String clave) {
        Object v = payload.get(clave);
        if (v == null) return 0.0;
        if (v instanceof Number n) return n.doubleValue();
        return Double.parseDouble(String.valueOf(v));
    }

    /**
     * Envía el estado actual del juego al cliente.
     */
    private void enviarEstado() {
        Map<String, Object> estado = motor.getEstadoJuego();
        String json = JsonUtil.crearMensajeEstado(estado);
        enviarJson(json);
    }
    
    /**
     * Envía un mensaje de error al cliente.
     */
    private void enviarError(String mensajeError) {
        String json = JsonUtil.crearMensajeError(mensajeError);
        enviarJson(json);
    }
    
    /**
     * Desconecta el cliente y limpia recursos.
     */
    private void desconectar() {
        conectado = false;

        // Eliminar del motor de juego según tipo
        if (jugadorId != null) {
            if (tipoCliente == TipoCliente.PLAYER) {
                motor.eliminarJugador(jugadorId);
                LoggerUtil.info("jugador " + jugadorId + " desconectado");
            } else if (tipoCliente == TipoCliente.SPECTATOR) {
                motor.eliminarEspectador(jugadorId);
                LoggerUtil.info("espectador " + jugadorId + " desconectado");
            } else if (tipoCliente == TipoCliente.ADMIN) {
                LoggerUtil.info("administrador " + jugadorId + " desconectado");
            }
        }

        // Eliminar como observador
        motor.eliminarObservador(this);

        // Cerrar socket
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            LoggerUtil.error("error al cerrar socket: " + e.getMessage());
        }
    }
    
    /**
     * Implementación de GameObserver: se llama cuando el MotorJuego notifica cambios.
     */
    @Override
    public void actualizar(Object dato) {
        if (!conectado) return;
        
        try {
            // Si es un evento específico, enviarlo
            if (dato instanceof EventoJuego) {
                EventoJuego evento = (EventoJuego) dato;
                enviarEvento(evento);
            } else {
                enviarEstado();
            }
        } catch (Exception e) {
            LoggerUtil.error("error al actualizar cliente: " + e.getMessage());
        }
    }

    private void enviarEvento(EventoJuego evento) {
        String json = JsonUtil.crearMensajeEvento(evento.getTipo().toString(), evento.getPayload());
        enviarJson(json);
    }

    private void enviarJson(String json) {
        if (json == null || salida == null || !conectado) {
            return;
        }
        synchronized (salidaLock) {
            salida.println(json);
        }
    }
}
