package cr.ac.tec.spaceinvaders.server.red;

import java.util.Map;

/**
 * Representa un mensaje JSON entre cliente y servidor.
 * Clase auxiliar para serialización/deserialización de mensajes.
 */
public class Mensaje {
    public enum TipoMensaje {
        INPUT,      // Input del jugador
        STATE,      // Estado del juego
        EVENT,      // Evento del juego
        CONNECT,    // Conexión de cliente
        DISCONNECT, // Desconexión
        ERROR       // Mensaje de error
    }
    
    private TipoMensaje type;
    private String id;
    private String action;
    private String name;
    private String clientType;  // "PLAYER" o "SPECTATOR"
    private String target;      // id del jugador observado (solo en CONNECT de SPECTATOR)
    private Object payload;
    private Map<String, Object> data;
    
    /**
     * Constructor vacío para Gson.
     */
    public Mensaje() {
    }
    
    /**
     * Constructor para mensajes de input.
     */
    public Mensaje(TipoMensaje type, String id, String action) {
        this.type = type;
        this.id = id;
        this.action = action;
    }
    
    /**
     * Constructor para mensajes de estado.
     */
    public Mensaje(TipoMensaje type, Map<String, Object> data) {
        this.type = type;
        this.data = data;
    }
    
    /**
     * Constructor para mensajes de evento.
     */
    public Mensaje(TipoMensaje type, String name, Object payload) {
        this.type = type;
        this.name = name;
        this.payload = payload;
    }
    
    // Getters y Setters
    public TipoMensaje getType() {
        return type;
    }
    
    public void setType(TipoMensaje type) {
        this.type = type;
    }
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getAction() {
        return action;
    }
    
    public void setAction(String action) {
        this.action = action;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public Object getPayload() {
        return payload;
    }
    
    public void setPayload(Object payload) {
        this.payload = payload;
    }
    
    public Map<String, Object> getData() {
        return data;
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
    }

    public String getClientType() {
        return clientType;
    }

    public void setClientType(String clientType) {
        this.clientType = clientType;
    }

    /**
     * Devuelve el id del jugador observado por el espectador.
     *
     * <p>Solo aplica a mensajes {@code CONNECT} con {@code clientType = "SPECTATOR"}.
     * Para otros tipos de mensaje este campo es {@code null}.</p>
     *
     * @return id del jugador objetivo o {@code null} si no aplica.
     */
    public String getTarget() {
        return target;
    }

    /**
     * Fija el id del jugador objetivo del espectador.
     *
     * @param target id del jugador a observar.
     */
    public void setTarget(String target) {
        this.target = target;
    }
}

