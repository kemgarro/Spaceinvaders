package cr.ac.tec.spaceinvaders.server.estructuras;

import org.junit.jupiter.api.Test;

import java.util.Iterator;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas unitarias para {@link ListaEnlazada}.
 *
 * <p>Cubren los casos de uso esperados por el motor del juego: agregar al
 * inicio y al final, eliminar en distintas posiciones, iteración con
 * {@code for-each}, y el patrón "recolectar y eliminar después" que
 * utilizará {@code DetectorColisiones}.</p>
 */
public class ListaEnlazadaTest {

    @Test
    public void listaNuevaEstaVacia() {
        ListaEnlazada<String> lista = new ListaEnlazada<>();
        assertTrue(lista.estaVacia(), "Una lista recién creada debe estar vacía");
        assertEquals(0, lista.tamano(), "El tamaño inicial debe ser cero");
    }

    @Test
    public void agregarMantieneOrdenDeInsercion() {
        ListaEnlazada<String> lista = new ListaEnlazada<>();
        lista.agregar("a");
        lista.agregar("b");
        lista.agregar("c");

        assertEquals(3, lista.tamano());
        assertEquals("a", lista.obtener(0));
        assertEquals("b", lista.obtener(1));
        assertEquals("c", lista.obtener(2));
        assertFalse(lista.estaVacia());
    }

    @Test
    public void agregarAlInicioInvierteOrden() {
        ListaEnlazada<Integer> lista = new ListaEnlazada<>();
        lista.agregarAlInicio(1);
        lista.agregarAlInicio(2);
        lista.agregarAlInicio(3);

        assertEquals(3, lista.tamano());
        assertEquals(3, lista.obtener(0));
        assertEquals(2, lista.obtener(1));
        assertEquals(1, lista.obtener(2));
    }

    @Test
    public void eliminarElementoExistenteReduceTamano() {
        ListaEnlazada<String> lista = new ListaEnlazada<>();
        lista.agregar("x");
        lista.agregar("y");
        lista.agregar("z");

        assertTrue(lista.eliminar("y"));
        assertEquals(2, lista.tamano());
        assertEquals("x", lista.obtener(0));
        assertEquals("z", lista.obtener(1));
    }

    @Test
    public void eliminarElementoInexistenteRetornaFalso() {
        ListaEnlazada<String> lista = new ListaEnlazada<>();
        lista.agregar("x");
        lista.agregar("y");

        assertFalse(lista.eliminar("z"));
        assertEquals(2, lista.tamano());
    }

    @Test
    public void eliminarCabezaActualizaInicio() {
        ListaEnlazada<Integer> lista = new ListaEnlazada<>();
        lista.agregar(10);
        lista.agregar(20);
        lista.agregar(30);

        assertTrue(lista.eliminar(10));
        assertEquals(2, lista.tamano());
        assertEquals(20, lista.obtener(0));
        assertEquals(30, lista.obtener(1));
    }

    @Test
    public void eliminarElementoEnMedioConservaExtremos() {
        ListaEnlazada<Integer> lista = new ListaEnlazada<>();
        lista.agregar(10);
        lista.agregar(20);
        lista.agregar(30);

        assertTrue(lista.eliminar(20));
        assertEquals(2, lista.tamano());
        assertEquals(10, lista.obtener(0));
        assertEquals(30, lista.obtener(1));
    }

    @Test
    public void eliminarUltimoNoRompeLista() {
        ListaEnlazada<Integer> lista = new ListaEnlazada<>();
        lista.agregar(10);
        lista.agregar(20);
        lista.agregar(30);

        assertTrue(lista.eliminar(30));
        assertEquals(2, lista.tamano());
        assertEquals(10, lista.obtener(0));
        assertEquals(20, lista.obtener(1));
        assertFalse(lista.contiene(30));
    }

    @Test
    public void eliminarNullBuscaNodoConValorNull() {
        ListaEnlazada<String> lista = new ListaEnlazada<>();
        lista.agregar("a");
        lista.agregar(null);
        lista.agregar("c");

        assertTrue(lista.eliminar(null));
        assertEquals(2, lista.tamano());
        assertEquals("a", lista.obtener(0));
        assertEquals("c", lista.obtener(1));
        assertFalse(lista.contiene(null));
    }

    @Test
    public void contieneDetectaPresenciaYAusencia() {
        ListaEnlazada<String> lista = new ListaEnlazada<>();
        lista.agregar("alpha");
        lista.agregar("beta");

        assertTrue(lista.contiene("alpha"));
        assertTrue(lista.contiene("beta"));
        assertFalse(lista.contiene("gamma"));
        assertFalse(lista.contiene(null));
    }

    @Test
    public void obtenerConIndiceNegativoLanzaExcepcion() {
        ListaEnlazada<String> lista = new ListaEnlazada<>();
        lista.agregar("a");
        assertThrows(IndexOutOfBoundsException.class, () -> lista.obtener(-1));
    }

    @Test
    public void obtenerConIndiceFueraDeRangoLanzaExcepcion() {
        ListaEnlazada<String> lista = new ListaEnlazada<>();
        lista.agregar("a");
        lista.agregar("b");
        assertThrows(IndexOutOfBoundsException.class, () -> lista.obtener(2));
        assertThrows(IndexOutOfBoundsException.class, () -> lista.obtener(100));
    }

    @Test
    public void obtenerEnListaVaciaLanzaExcepcion() {
        ListaEnlazada<String> lista = new ListaEnlazada<>();
        assertThrows(IndexOutOfBoundsException.class, () -> lista.obtener(0));
    }

    @Test
    public void vaciarDejaListaVacia() {
        ListaEnlazada<Integer> lista = new ListaEnlazada<>();
        lista.agregar(1);
        lista.agregar(2);
        lista.agregar(3);

        lista.vaciar();
        assertEquals(0, lista.tamano());
        assertTrue(lista.estaVacia());
        assertFalse(lista.contiene(1));
    }

    @Test
    public void forEachRecorreElementosEnOrden() {
        ListaEnlazada<Integer> lista = new ListaEnlazada<>();
        lista.agregar(1);
        lista.agregar(2);
        lista.agregar(3);

        StringBuilder traza = new StringBuilder();
        for (Integer valor : lista) {
            traza.append(valor);
        }
        assertEquals("123", traza.toString());
    }

    @Test
    public void iteradorLanzaExcepcionDespuesDelUltimo() {
        ListaEnlazada<String> lista = new ListaEnlazada<>();
        lista.agregar("uno");

        Iterator<String> it = lista.iterator();
        assertTrue(it.hasNext());
        assertEquals("uno", it.next());
        assertFalse(it.hasNext());
        assertThrows(NoSuchElementException.class, it::next);
    }

    @Test
    public void iteradorEnListaVaciaNoTieneElementos() {
        ListaEnlazada<String> lista = new ListaEnlazada<>();
        Iterator<String> it = lista.iterator();
        assertFalse(it.hasNext());
        assertThrows(NoSuchElementException.class, it::next);
    }

    @Test
    public void toStringMuestraFormatoEsperado() {
        ListaEnlazada<Integer> lista = new ListaEnlazada<>();
        assertEquals("[]", lista.toString());

        lista.agregar(1);
        lista.agregar(2);
        lista.agregar(3);
        assertEquals("[1, 2, 3]", lista.toString());
    }

    /**
     * Valida el patrón "recolectar y eliminar después" que utilizará el
     * detector de colisiones del juego: recorrer la lista, identificar los
     * elementos a eliminar acumulándolos en otra lista, y borrarlos en una
     * segunda pasada para no romper el iterador.
     */
    @Test
    public void patronRecolectarYEliminarDespues() {
        ListaEnlazada<Integer> balas = new ListaEnlazada<>();
        for (int i = 1; i <= 6; i++) {
            balas.agregar(i);
        }

        // Primera pasada: recolectar los pares (simulando "impactaron").
        ListaEnlazada<Integer> aEliminar = new ListaEnlazada<>();
        for (Integer bala : balas) {
            if (bala % 2 == 0) {
                aEliminar.agregarAlInicio(bala);
            }
        }
        assertEquals(3, aEliminar.tamano());

        // Segunda pasada: eliminar de la lista original.
        for (Integer bala : aEliminar) {
            assertTrue(balas.eliminar(bala),
                "Cada elemento marcado debería existir en la lista original");
        }

        assertEquals(3, balas.tamano());
        assertEquals(1, balas.obtener(0));
        assertEquals(3, balas.obtener(1));
        assertEquals(5, balas.obtener(2));
        assertFalse(balas.contiene(2));
        assertFalse(balas.contiene(4));
        assertFalse(balas.contiene(6));
    }
}
