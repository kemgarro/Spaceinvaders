package cr.ac.tec.spaceinvaders.server.observador;

/**
 * Interfaz para el patrón Observer.
 * Permite que los objetos sean notificados de cambios en los sujetos.
 */
public interface GameObserver {
    /**
     * Método llamado cuando el sujeto notifica un cambio.
     * @param dato datos adicionales del cambio (puede ser null)
     */
    void actualizar(Object dato);
}
