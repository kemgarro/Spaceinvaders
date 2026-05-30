package cr.ac.tec.spaceinvaders.server.red;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import cr.ac.tec.spaceinvaders.server.util.LoggerUtil;

/**
 * Utilidad para serialización y deserialización de mensajes JSON.
 * Usa Gson para convertir objetos Java a JSON y viceversa.
 */
public class JsonUtil {
    private static final Gson gson = new GsonBuilder()
            .create();
    
    /**
     * Convierte un objeto Mensaje a JSON.
     */
    public static String toJson(Mensaje mensaje) {
        try {
            return gson.toJson(mensaje);
        } catch (Exception e) {
            LoggerUtil.error("error al serializar mensaje: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Convierte un objeto genérico a JSON.
     */
    public static String toJson(Object obj) {
        try {
            return gson.toJson(obj);
        } catch (Exception e) {
            LoggerUtil.error("error al serializar objeto: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Convierte un JSON a un objeto Mensaje.
     */
    public static Mensaje fromJson(String json) {
        try {
            return gson.fromJson(json, Mensaje.class);
        } catch (JsonSyntaxException e) {
            LoggerUtil.error("error al deserializar json: " + e.getMessage());
            LoggerUtil.debug("json recibido: " + json);
            return null;
        }
    }
    
    /**
     * Convierte un JSON a un objeto de la clase especificada.
     */
    public static <T> T fromJson(String json, Class<T> clazz) {
        try {
            return gson.fromJson(json, clazz);
        } catch (JsonSyntaxException e) {
            LoggerUtil.error("error al deserializar json a " + clazz.getSimpleName() + ": " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Crea un mensaje de estado del juego.
     */
    public static String crearMensajeEstado(java.util.Map<String, Object> estado) {
        Mensaje mensaje = new Mensaje(Mensaje.TipoMensaje.STATE, estado);
        return toJson(mensaje);
    }
    
    /**
     * Crea un mensaje de evento.
     */
    public static String crearMensajeEvento(String nombreEvento, Object payload) {
        Mensaje mensaje = new Mensaje(Mensaje.TipoMensaje.EVENT, nombreEvento, payload);
        return toJson(mensaje);
    }
    
    /**
     * Crea un mensaje de error.
     */
    public static String crearMensajeError(String mensajeError) {
        Mensaje mensaje = new Mensaje();
        mensaje.setType(Mensaje.TipoMensaje.ERROR);
        java.util.Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("error", mensajeError);
        mensaje.setPayload(payload);
        return toJson(mensaje);
    }
}

