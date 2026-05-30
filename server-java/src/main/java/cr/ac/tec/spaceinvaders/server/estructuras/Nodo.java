package cr.ac.tec.spaceinvaders.server.estructuras;

/**
 * Nodo de una lista simplemente enlazada.
 *
 * <p>Representa una celda individual dentro de {@link ListaEnlazada}.
 * Cada nodo almacena un valor genérico y un puntero al siguiente nodo
 * de la lista. Si {@code siguiente} es {@code null}, el nodo es el último.</p>
 *
 * <p>Esta clase es interna a la estructura de datos propia del servidor
 * (requisito RNF-11 / RNF-12 del enunciado) y no debe exponerse fuera
 * del paquete {@code estructuras} salvo para fines de prueba.</p>
 *
 * @param <T> tipo del valor almacenado en el nodo.
 */
public class Nodo<T> {

    /** Valor almacenado en este nodo. */
    private T valor;

    /** Referencia al siguiente nodo de la lista, o {@code null} si es el último. */
    private Nodo<T> siguiente;

    /**
     * Construye un nodo nuevo con el valor indicado y sin sucesor.
     *
     * @param valor valor a almacenar (puede ser {@code null}).
     */
    public Nodo(T valor) {
        this.valor = valor;
        this.siguiente = null;
    }

    /**
     * Obtiene el valor almacenado en el nodo.
     *
     * @return valor del nodo.
     */
    public T getValor() {
        return valor;
    }

    /**
     * Reemplaza el valor almacenado en el nodo.
     *
     * @param valor nuevo valor.
     */
    public void setValor(T valor) {
        this.valor = valor;
    }

    /**
     * Obtiene el siguiente nodo de la lista.
     *
     * @return siguiente nodo o {@code null} si es el último.
     */
    public Nodo<T> getSiguiente() {
        return siguiente;
    }

    /**
     * Establece el siguiente nodo de la lista.
     *
     * @param siguiente nodo sucesor o {@code null} para marcar el fin.
     */
    public void setSiguiente(Nodo<T> siguiente) {
        this.siguiente = siguiente;
    }
}
