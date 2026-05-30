package cr.ac.tec.spaceinvaders.server.nucleo;

import cr.ac.tec.spaceinvaders.server.entidades.Bunker;
import cr.ac.tec.spaceinvaders.server.entidades.Jugador;
import cr.ac.tec.spaceinvaders.server.entidades.TipoAlien;
import cr.ac.tec.spaceinvaders.server.eventos.EventoJuego;
import cr.ac.tec.spaceinvaders.server.fabricas.FabricaAliens;
import cr.ac.tec.spaceinvaders.server.fabricas.FabricaAliensEstandar;
import cr.ac.tec.spaceinvaders.server.util.Config;
import cr.ac.tec.spaceinvaders.server.util.GeneradorIds;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Encargado de configurar las oleadas y bunkers de la partida.
 *
 * <p>Responsabilidades:</p>
 * <ul>
 *   <li>{@link #prepararInicio(EstadoJuego)}: crea los 4 bunkers y la primera
 *       oleada con la formación clásica de 5x11 aliens.</li>
 *   <li>{@link #siguienteOleada(EstadoJuego, DetectorColisiones.SinkEventos)}:
 *       transición entre oleadas — +1 vida a cada jugador, reduce el
 *       intervalo de movimiento de los aliens y regenera la formación.</li>
 * </ul>
 *
 * <p>El layout 5x11 utiliza la distribución estándar de Space Invaders:</p>
 * <ul>
 *   <li>Fila 0: {@link TipoAlien#SQUID} (10 puntos).</li>
 *   <li>Filas 1–2: {@link TipoAlien#CRAB} (20 puntos).</li>
 *   <li>Filas 3–4: {@link TipoAlien#OCTOPUS} (40 puntos).</li>
 * </ul>
 *
 * <p>La eleccion del tipo concreto de alien se delega a una
 * {@link FabricaAliens}, inyectada por constructor para permitir variantes
 * en el futuro (p. ej. una fabrica "futurista" para skins distintos).</p>
 */
public class GestorOleadas {

    /** Número de filas en la formación de aliens. */
    private static final int FILAS = 5;

    /** Número de columnas en la formación. */
    private static final int COLUMNAS = 11;

    private static final double MARGEN_X = 60.0;
    private static final double MARGEN_Y = 60.0;
    private static final double SEP_X = 50.0;
    private static final double SEP_Y = 35.0;

    private final FabricaAliens fabrica;

    /** Constructor por defecto: usa la {@link FabricaAliensEstandar}. */
    public GestorOleadas() {
        this(new FabricaAliensEstandar());
    }

    /** Constructor con inyeccion de la fabrica de aliens a utilizar. */
    public GestorOleadas(FabricaAliens fabrica) {
        this.fabrica = fabrica;
    }

    /**
     * Configura el estado de juego inicial: 4 bunkers + primera oleada.
     *
     * @param estado estado a inicializar.
     */
    public void prepararInicio(EstadoJuego estado) {
        estado.oleada = 0;
        estado.intervaloAliensMs = Config.ALIENS_INTERVALO_BASE_MS;
        crearBunkers(estado);
        generarOleada(estado);
    }

    /**
     * Avanza a la siguiente oleada.
     *
     * <p>Otorga +1 vida a cada jugador (emite
     * {@link EventoJuego.TipoEvento#PLAYER_LIFE_GAINED}), reduce el intervalo
     * de movimiento de aliens según {@link Config#ALIENS_REDUCCION_POR_OLEADA}
     * (sin bajar de {@link Config#ALIENS_INTERVALO_MIN_MS}, emite
     * {@link EventoJuego.TipoEvento#SPEED_CHANGED}), regenera la formación 5x11
     * y emite {@link EventoJuego.TipoEvento#WAVE_STARTED}.</p>
     *
     * @param estado estado actual.
     * @param sink   destino de eventos.
     */
    public void siguienteOleada(EstadoJuego estado, DetectorColisiones.SinkEventos sink) {
        for (Jugador j : estado.jugadores) {
            j.ganarVida();
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("playerId", j.getId());
            payload.put("vidas", j.getVidas());
            sink.emitir(new EventoJuego(EventoJuego.TipoEvento.PLAYER_LIFE_GAINED, payload));
        }

        long nuevoIntervalo = (long) Math.max(
            Config.ALIENS_INTERVALO_MIN_MS,
            estado.intervaloAliensMs * (1.0 - Config.ALIENS_REDUCCION_POR_OLEADA)
        );
        if (nuevoIntervalo != estado.intervaloAliensMs) {
            estado.intervaloAliensMs = nuevoIntervalo;
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("intervaloMs", nuevoIntervalo);
            sink.emitir(new EventoJuego(EventoJuego.TipoEvento.SPEED_CHANGED, payload));
        }

        generarOleada(estado);

        Map<String, Object> wavePayload = new LinkedHashMap<>();
        wavePayload.put("oleada", estado.oleada);
        sink.emitir(new EventoJuego(EventoJuego.TipoEvento.WAVE_STARTED, wavePayload));
    }

    /** Genera la formación 5x11 y la pone en {@code estado.aliens}. */
    private void generarOleada(EstadoJuego estado) {
        estado.oleada++;
        estado.aliens.vaciar();
        for (int fila = 0; fila < FILAS; fila++) {
            for (int col = 0; col < COLUMNAS; col++) {
                double x = MARGEN_X + col * SEP_X;
                double y = MARGEN_Y + fila * SEP_Y;
                estado.aliens.agregar(fabrica.crearAlien(fila, x, y));
            }
        }
    }

    /** Crea los {@link Config#NUM_BUNKERS} bunkers espaciados uniformemente. */
    private void crearBunkers(EstadoJuego estado) {
        estado.bunkers.vaciar();
        double sep = Config.CAMPO_ANCHO / (Config.NUM_BUNKERS + 1.0);
        double y = Config.CANNON_Y - 80.0;
        for (int i = 0; i < Config.NUM_BUNKERS; i++) {
            double x = sep * (i + 1) - Config.ANCHO_BUNKER / 2.0;
            estado.bunkers.agregar(new Bunker(GeneradorIds.siguiente("BK"), x, y));
        }
    }
}
