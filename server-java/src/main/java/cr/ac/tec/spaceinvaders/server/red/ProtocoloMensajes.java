package cr.ac.tec.spaceinvaders.server.red;

import java.util.Map;

/**
 * Fachada de alto nivel sobre el protocolo de mensajes JSON.
 * Resuelve los dos casos mas comunes:
 *   - serializar el estado del juego para enviarlo a los clientes.
 *   - parsear una linea de input que llega del cliente.
 *
 * Internamente delega en {@link JsonUtil} y {@link Mensaje}.
 */
public final class ProtocoloMensajes {
    private ProtocoloMensajes() {}

    /** Serializa un snapshot del estado a una linea JSON tipo STATE. */
    public static String serializar(Map<String, Object> estado) {
        return JsonUtil.crearMensajeEstado(estado);
    }

    /**
     * Parsea una linea JSON que se espera sea de tipo INPUT.
     * @return arreglo {jugadorId, accion} o null si no era INPUT valido.
     */
    public static String[] parsearInput(String linea) {
        Mensaje m = JsonUtil.fromJson(linea);
        if (m == null) return null;
        if (m.getType() != Mensaje.TipoMensaje.INPUT) return null;
        if (m.getId() == null || m.getAction() == null) return null;
        return new String[]{ m.getId(), m.getAction() };
    }
}
