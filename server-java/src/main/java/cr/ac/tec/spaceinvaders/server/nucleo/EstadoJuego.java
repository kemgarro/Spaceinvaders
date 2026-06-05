package cr.ac.tec.spaceinvaders.server.nucleo;

import cr.ac.tec.spaceinvaders.server.entidades.Alien;
import cr.ac.tec.spaceinvaders.server.entidades.Bala;
import cr.ac.tec.spaceinvaders.server.entidades.Bunker;
import cr.ac.tec.spaceinvaders.server.entidades.Canon;
import cr.ac.tec.spaceinvaders.server.entidades.Jugador;
import cr.ac.tec.spaceinvaders.server.entidades.Ovni;
import cr.ac.tec.spaceinvaders.server.estructuras.ListaEnlazada;
import cr.ac.tec.spaceinvaders.server.util.Config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Contenedor mutable del estado actual de la partida.
 *
 * <p>Agrupa todas las colecciones de entidades vivas del motor (aliens, balas,
 * bunkers, cañones, jugadores y, opcionalmente, OVNI), más metadatos de la
 * oleada en curso. Las colecciones de entidades se almacenan en
 * {@link ListaEnlazada} porque los requisitos RNF-11/RNF-12 prohíben usar
 * colecciones del JDK como almacenes principales.</p>
 *
 * <p>Esta clase no realiza lógica: solo es estado. El {@link MotorJuego},
 * el {@link GestorOleadas} y el {@link DetectorColisiones} son quienes la
 * modifican.</p>
 *
 * <p>El método {@link #aMapa()} produce una vista de solo lectura del estado
 * en forma de {@link LinkedHashMap}, lista para ser serializada a JSON por
 * la capa de red ({@code JsonUtil.crearMensajeEstado}).</p>
 */
public class EstadoJuego {

    /** Aliens vivos en la oleada actual. */
    public final ListaEnlazada<Alien> aliens = new ListaEnlazada<>();

    /** Balas activas (de jugadores y aliens). */
    public final ListaEnlazada<Bala> balas = new ListaEnlazada<>();

    /** Bunkers (escudos) en pantalla. */
    public final ListaEnlazada<Bunker> bunkers = new ListaEnlazada<>();

    /** Cañones activos (uno por jugador conectado). */
    public final ListaEnlazada<Canon> canones = new ListaEnlazada<>();

    /** Jugadores conectados (vivos o no). */
    public final ListaEnlazada<Jugador> jugadores = new ListaEnlazada<>();

    /** Identificadores de espectadores conectados. */
    public final Set<String> espectadores = ConcurrentHashMap.newKeySet();

    /**
     * Relacion espectador -> jugador observado.
     *
     * <p>La clave es el id del espectador y el valor es el id del jugador al
     * que esta asociado. Cada espectador observa a un unico jugador (RF-CE03
     * del enunciado: "cada jugador tiene su propio espectador asociado").</p>
     */
    public final Map<String, String> espectadoresPorJugador = new ConcurrentHashMap<>();

    /** OVNI activo, o {@code null} si no hay ninguno cruzando la pantalla. */
    public Ovni ovni;

    /** Número de oleada actual (comienza en 1 tras {@link GestorOleadas#prepararInicio}). */
    public int oleada = 0;

    /**
     * Intervalo BASE (ms) entre pasos del bloque de aliens para la oleada
     * actual. El intervalo efectivo se reduce a medida que mueren aliens
     * (ver {@code MotorJuego.calcularIntervaloEfectivoAliensMs}), siguiendo
     * la mecánica clásica de Space Invaders donde los pocos sobrevivientes
     * se aceleran. Entre oleadas, este valor BASE se reduce un porcentaje
     * fijo (ver {@link Config#ALIENS_REDUCCION_POR_OLEADA}).
     */
    public long intervaloAliensMs = Config.ALIENS_INTERVALO_BASE_MS;

    /**
     * Cantidad de aliens generados al inicio de la oleada actual. Se usa
     * como denominador para escalar el intervalo de movimiento conforme
     * la población de aliens disminuye. Lo fija {@link GestorOleadas}.
     */
    public int aliensInicialesOleada = 0;

    /**
     * Acumulador (ms) usado por el motor para decidir cuándo toca mover
     * el bloque de aliens. Suma el delta de cada tick; cuando supera el
     * intervalo efectivo, se mueven los aliens y se resta. Resetea entre
     * oleadas. NO se serializa al cliente (es estado interno del motor).
     */
    public long acumuladorAliensMs = 0;

    /** Bandera global de fin de juego. */
    public boolean juegoTerminado = false;

    /**
     * Serializa el estado actual a un {@link LinkedHashMap} listo para JSON.
     *
     * <p>Solo se incluyen entidades vivas (excepto jugadores, que se reportan
     * siempre con su estado de vidas para que la UI pueda mostrar "ELIMINADO").
     * El orden de las claves está fijo gracias al {@code LinkedHashMap}.</p>
     *
     * @return mapa con las claves {@code oleada}, {@code juegoTerminado},
     *         {@code intervaloAliensMs}, {@code aliensInicialesOleada},
     *         {@code aliens}, {@code balas}, {@code bunkers}, {@code canones},
     *         {@code ovni}, {@code jugadores}.
     */
    public Map<String, Object> aMapa() {
        Map<String, Object> mapa = new LinkedHashMap<>();
        mapa.put("oleada", oleada);
        mapa.put("juegoTerminado", juegoTerminado);
        mapa.put("intervaloAliensMs", intervaloAliensMs);
        mapa.put("aliensInicialesOleada", aliensInicialesOleada);

        List<Map<String, Object>> aliensJson = new ArrayList<>();
        for (Alien a : aliens) {
            if (!a.estaVivo()) continue;
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", a.getId());
            m.put("tipo", a.getTipo().name());
            m.put("x", a.getX());
            m.put("y", a.getY());
            m.put("puntos", a.getPuntos());
            aliensJson.add(m);
        }
        mapa.put("aliens", aliensJson);

        List<Map<String, Object>> balasJson = new ArrayList<>();
        for (Bala b : balas) {
            if (!b.estaVivo()) continue;
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", b.getId());
            m.put("x", b.getX());
            m.put("y", b.getY());
            m.put("direccion", b.getDireccion().name());
            m.put("duenio", b.getDuenioId());
            balasJson.add(m);
        }
        mapa.put("balas", balasJson);

        List<Map<String, Object>> bunkersJson = new ArrayList<>();
        for (Bunker bk : bunkers) {
            if (!bk.estaVivo()) continue;
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", bk.getId());
            m.put("x", bk.getX());
            m.put("y", bk.getY());
            m.put("salud", bk.getSalud());
            m.put("estadoVisible", bk.getEstadoVisible());
            bunkersJson.add(m);
        }
        mapa.put("bunkers", bunkersJson);

        List<Map<String, Object>> canonesJson = new ArrayList<>();
        for (Canon c : canones) {
            if (!c.estaVivo()) continue;
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", c.getId());
            m.put("jugadorId", c.getJugadorId());
            m.put("x", c.getX());
            m.put("y", c.getY());
            canonesJson.add(m);
        }
        mapa.put("canones", canonesJson);

        if (ovni != null && ovni.estaVivo()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", ovni.getId());
            m.put("x", ovni.getX());
            m.put("y", ovni.getY());
            m.put("direccion", ovni.getDireccion().name());
            m.put("puntosBase", ovni.getPuntosBase());
            mapa.put("ovni", m);
        } else {
            mapa.put("ovni", null);
        }

        List<Map<String, Object>> jugadoresJson = new ArrayList<>();
        for (Jugador j : jugadores) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", j.getId());
            m.put("nombre", j.getNombre());
            m.put("puntaje", j.getPuntaje());
            m.put("vidas", j.getVidas());
            jugadoresJson.add(m);
        }
        mapa.put("jugadores", jugadoresJson);

        // Mapeo espectador -> jugador observado.
        // Se serializa como LinkedHashMap para que el orden de las claves sea
        // estable en el JSON resultante (util para diagnostico).
        mapa.put("espectadoresPorJugador", new LinkedHashMap<>(espectadoresPorJugador));

        return mapa;
    }
}
