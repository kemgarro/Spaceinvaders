package cr.ac.tec.spaceinvaders.server.observador;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Clase base para el patrón Observer.
 * Permite que los sujetos notifiquen cambios a sus observadores.
 */
public abstract class Sujeto {
    private List<GameObserver> observadores;

    /**
     * Constructor del sujeto.
     */
    public Sujeto() {
        this.observadores = new CopyOnWriteArrayList<>();
    }

    /**
     * Agrega un observador a la lista.
     */
    public void agregarObservador(GameObserver observador) {
        observadores.add(observador);
    }

    /**
     * Elimina un observador de la lista.
     */
    public void eliminarObservador(GameObserver observador) {
        observadores.remove(observador);
    }

    /**
     * Notifica a todos los observadores de un cambio.
     */
    protected void notificar(Object dato) {
        for (GameObserver observador : observadores) {
            observador.actualizar(dato);
        }
    }

    /**
     * Notifica a todos los observadores sin datos adicionales.
     */
    protected void notificar() {
        notificar(null);
    }
}
