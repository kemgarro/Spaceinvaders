package cr.ac.tec.spaceinvaders.server.estructuras;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Lista simplemente enlazada genérica, implementada desde cero.
 *
 * <h2>Motivación</h2>
 * <p>Los requisitos no funcionales RNF-11 y RNF-12 del enunciado de
 * spaCEinvaders exigen que las estructuras de datos utilizadas por el
 * motor del juego sean implementadas y documentadas por el equipo, sin
 * apoyarse en colecciones del JDK como {@code java.util.LinkedList} o
 * {@code java.util.ArrayList}. Esta clase cubre ese requisito y será
 * utilizada para almacenar aliens, balas, bunkers y jugadores dentro
 * de {@code EstadoJuego}. La lista se recorre aproximadamente veinte
 * veces por segundo durante el ciclo de juego.</p>
 *
 * <h2>Estructura interna</h2>
 * <p>Se mantiene un puntero a la {@code cabeza} de la lista (primer
 * {@link Nodo}) y un contador {@code tamano} con la cantidad de
 * elementos. Cada nodo conoce únicamente a su sucesor, por lo que es
 * una lista <em>simplemente</em> enlazada. No se mantiene un puntero a
 * la cola para preservar la simplicidad de la implementación.</p>
 *
 * <h2>Complejidad de las operaciones</h2>
 * <ul>
 *   <li>{@link #agregar(Object)}: O(n) — recorre hasta el último nodo.</li>
 *   <li>{@link #agregarAlInicio(Object)}: O(1) — inserta delante de la cabeza.</li>
 *   <li>{@link #eliminar(Object)}: O(n) en el peor caso.</li>
 *   <li>{@link #contiene(Object)}: O(n).</li>
 *   <li>{@link #obtener(int)}: O(n).</li>
 *   <li>{@link #tamano()}, {@link #estaVacia()}, {@link #vaciar()}: O(1).</li>
 * </ul>
 *
 * <p>Cuando el orden de inserción no es relevante (por ejemplo, al
 * registrar nuevas balas en cada tick), preferir {@link #agregarAlInicio(Object)}
 * para evitar el costo lineal de {@link #agregar(Object)}.</p>
 *
 * <h2>Iteración y eliminación segura</h2>
 * <p>La clase implementa {@link Iterable}, por lo que puede usarse con
 * {@code for-each}. <strong>Advertencia importante:</strong> el iterador
 * devuelto es <em>fail-fast</em> en el sentido conceptual; modificar la
 * lista (especialmente con {@link #eliminar(Object)}) mientras se itera
 * romperá el recorrido y puede saltar elementos o lanzar
 * {@link NoSuchElementException}.</p>
 *
 * <p>El caso típico en el motor del juego es recorrer la lista de balas
 * o aliens para detectar colisiones y luego eliminar las entidades
 * impactadas. Para hacerlo de forma segura se debe usar el patrón
 * <em>recolectar y eliminar después</em>:</p>
 *
 * <pre>
 *   ListaEnlazada&lt;Bala&gt; aEliminar = new ListaEnlazada&lt;&gt;();
 *   for (Bala b : balas) {
 *       if (b.impacto()) {
 *           aEliminar.agregarAlInicio(b);
 *       }
 *   }
 *   for (Bala b : aEliminar) {
 *       balas.eliminar(b);
 *   }
 * </pre>
 *
 * <p>Este patrón es el que utilizará el {@code DetectorColisiones} para
 * mantener consistencia durante los recorridos del ciclo de juego.</p>
 *
 * @param <T> tipo de los elementos almacenados.
 */
public class ListaEnlazada<T> implements Iterable<T> {

    /** Primer nodo de la lista, o {@code null} si está vacía. */
    private Nodo<T> cabeza;

    /** Cantidad de elementos actualmente almacenados. */
    private int tamano;

    /**
     * Construye una lista vacía.
     */
    public ListaEnlazada() {
        this.cabeza = null;
        this.tamano = 0;
    }

    /**
     * Inserta un elemento al final de la lista.
     *
     * <p>Complejidad: O(n) porque recorre la lista hasta el último nodo.
     * Si la posición exacta no es relevante, considere
     * {@link #agregarAlInicio(Object)}.</p>
     *
     * @param valor valor a insertar (puede ser {@code null}).
     */
    public void agregar(T valor) {
        Nodo<T> nuevo = new Nodo<>(valor);
        if (cabeza == null) {
            cabeza = nuevo;
        } else {
            Nodo<T> actual = cabeza;
            while (actual.getSiguiente() != null) {
                actual = actual.getSiguiente();
            }
            actual.setSiguiente(nuevo);
        }
        tamano++;
    }

    /**
     * Inserta un elemento al inicio de la lista.
     *
     * <p>Complejidad: O(1).</p>
     *
     * @param valor valor a insertar (puede ser {@code null}).
     */
    public void agregarAlInicio(T valor) {
        Nodo<T> nuevo = new Nodo<>(valor);
        nuevo.setSiguiente(cabeza);
        cabeza = nuevo;
        tamano++;
    }

    /**
     * Elimina la primera ocurrencia del valor indicado.
     *
     * <p>La comparación se realiza con {@link Object#equals(Object)}, y se
     * acepta {@code null} como argumento: en ese caso se elimina el primer
     * nodo cuyo valor sea {@code null}.</p>
     *
     * <p>Complejidad: O(n).</p>
     *
     * @param valor valor a eliminar (puede ser {@code null}).
     * @return {@code true} si se eliminó algún nodo, {@code false} si el
     *         valor no estaba en la lista.
     */
    public boolean eliminar(T valor) {
        if (cabeza == null) {
            return false;
        }

        if (sonIguales(cabeza.getValor(), valor)) {
            cabeza = cabeza.getSiguiente();
            tamano--;
            return true;
        }

        Nodo<T> previo = cabeza;
        Nodo<T> actual = cabeza.getSiguiente();
        while (actual != null) {
            if (sonIguales(actual.getValor(), valor)) {
                previo.setSiguiente(actual.getSiguiente());
                tamano--;
                return true;
            }
            previo = actual;
            actual = actual.getSiguiente();
        }
        return false;
    }

    /**
     * Indica si la lista contiene el valor indicado.
     *
     * <p>Acepta {@code null} y compara con {@link Object#equals(Object)}.</p>
     *
     * @param valor valor a buscar.
     * @return {@code true} si el valor está presente.
     */
    public boolean contiene(T valor) {
        Nodo<T> actual = cabeza;
        while (actual != null) {
            if (sonIguales(actual.getValor(), valor)) {
                return true;
            }
            actual = actual.getSiguiente();
        }
        return false;
    }

    /**
     * Obtiene el valor en la posición indicada.
     *
     * @param indice índice basado en cero.
     * @return valor almacenado en esa posición.
     * @throws IndexOutOfBoundsException si {@code indice < 0} o
     *         {@code indice >= tamano()}.
     */
    public T obtener(int indice) {
        if (indice < 0 || indice >= tamano) {
            throw new IndexOutOfBoundsException(
                "Índice fuera de rango: " + indice + " (tamaño = " + tamano + ")"
            );
        }
        Nodo<T> actual = cabeza;
        for (int i = 0; i < indice; i++) {
            actual = actual.getSiguiente();
        }
        return actual.getValor();
    }

    /**
     * Elimina todos los elementos de la lista.
     *
     * <p>Útil para reiniciar colecciones de entidades al iniciar una
     * nueva oleada o al terminar la partida.</p>
     */
    public void vaciar() {
        cabeza = null;
        tamano = 0;
    }

    /**
     * Cantidad actual de elementos.
     *
     * @return tamaño de la lista.
     */
    public int tamano() {
        return tamano;
    }

    /**
     * Indica si la lista no contiene elementos.
     *
     * @return {@code true} si el tamaño es cero.
     */
    public boolean estaVacia() {
        return tamano == 0;
    }

    /**
     * Retorna un iterador que recorre los elementos en orden de inserción.
     *
     * <p>El iterador no soporta modificación concurrente: ver el bloque
     * "Iteración y eliminación segura" en la documentación de la clase.</p>
     *
     * @return iterador sobre los valores almacenados.
     */
    @Override
    public Iterator<T> iterator() {
        return new IteradorLista();
    }

    /**
     * Representación textual con formato {@code [v1, v2, v3]} útil para
     * depuración.
     *
     * @return cadena con los elementos en orden.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("[");
        Nodo<T> actual = cabeza;
        boolean primero = true;
        while (actual != null) {
            if (!primero) {
                sb.append(", ");
            }
            sb.append(actual.getValor());
            primero = false;
            actual = actual.getSiguiente();
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * Compara dos valores manejando correctamente referencias {@code null}.
     *
     * @param a primer valor.
     * @param b segundo valor.
     * @return {@code true} si ambos son {@code null} o si {@code a.equals(b)}.
     */
    private boolean sonIguales(T a, T b) {
        if (a == null) {
            return b == null;
        }
        return a.equals(b);
    }

    /**
     * Iterador interno sobre la lista. Recorre desde la cabeza hacia el
     * final usando el enlace {@code siguiente} de cada nodo.
     */
    private class IteradorLista implements Iterator<T> {

        /** Próximo nodo a retornar, o {@code null} si ya se agotó la lista. */
        private Nodo<T> actual = cabeza;

        @Override
        public boolean hasNext() {
            return actual != null;
        }

        @Override
        public T next() {
            if (actual == null) {
                throw new NoSuchElementException(
                    "No hay más elementos en la lista."
                );
            }
            T valor = actual.getValor();
            actual = actual.getSiguiente();
            return valor;
        }
    }
}
