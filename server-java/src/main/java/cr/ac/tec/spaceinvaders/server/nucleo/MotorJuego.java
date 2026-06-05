package cr.ac.tec.spaceinvaders.server.nucleo;

import cr.ac.tec.spaceinvaders.server.entidades.Alien;
import cr.ac.tec.spaceinvaders.server.entidades.Bala;
import cr.ac.tec.spaceinvaders.server.entidades.Bunker;
import cr.ac.tec.spaceinvaders.server.entidades.Canon;
import cr.ac.tec.spaceinvaders.server.entidades.Jugador;
import cr.ac.tec.spaceinvaders.server.entidades.Ovni;
import cr.ac.tec.spaceinvaders.server.eventos.EventoJuego;
import cr.ac.tec.spaceinvaders.server.fabricas.FabricaAliensEstandar;
import cr.ac.tec.spaceinvaders.server.observador.Sujeto;
import cr.ac.tec.spaceinvaders.server.util.Config;
import cr.ac.tec.spaceinvaders.server.util.GeneradorIds;
import cr.ac.tec.spaceinvaders.server.util.LoggerUtil;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Motor principal del juego spaCEinvaders.
 *
 * <p>Es el <strong>ConcreteSubject</strong> del patron Observer (extiende
 * {@link Sujeto}) y la unica clase del paquete {@code nucleo} que las capas
 * superiores (red y CLI) deben conocer. Concentra dos responsabilidades:</p>
 *
 * <ol>
 *   <li><strong>API publica de orquestacion</strong>: alta/baja de jugadores
 *       y espectadores, snapshot del estado, procesamiento de inputs y avance
 *       del tick. Todos los puntos de entrada estan protegidos por un mutex
 *       global porque varios hilos los invocan (cada manejador de cliente
 *       vive en su propio hilo).</li>
 *   <li><strong>Logica interna del tick</strong>: movimiento de entidades,
 *       rebote del bloque de aliens, disparos aleatorios, limpieza de balas
 *       y entidades destruidas, deteccion de colisiones (delegada a
 *       {@link DetectorColisiones}) y transicion de oleada (delegada a
 *       {@link GestorOleadas}).</li>
 * </ol>
 *
 * <p>Las notificaciones a los observadores ({@code ManejadorCliente}) tambien
 * se hacen bajo el lock — esto es seguro porque los observadores solo
 * escriben al socket, no llaman de vuelta al motor.</p>
 *
 * <p>Durante un tick los eventos se acumulan en una lista temporal y se
 * envian despues de actualizar el estado, para evitar reentrancia y para que
 * los observadores reciban primero los eventos puntuales y luego el snapshot
 * consolidado.</p>
 */
public class MotorJuego extends Sujeto {

    /** Mutex global del estado. */
    private final Object lock = new Object();

    /** Estado mutable del juego. */
    private final EstadoJuego estado = new EstadoJuego();

    /** Detector de colisiones (helper interno del tick). */
    private final DetectorColisiones detector = new DetectorColisiones();

    /** Gestor de oleadas (formacion 5x11, bunkers, transicion entre oleadas). */
    private final GestorOleadas gestorOleadas = new GestorOleadas();

    /** Fabrica concreta utilizada para comandos administrativos (crear OVNI, etc.). */
    private final FabricaAliensEstandar fabricaConcreta = new FabricaAliensEstandar();

    /** Aleatorio interno para disparos de aliens. */
    private final Random aleatorio = new Random();

    /** Marca para evitar invertir aliens dos veces en el mismo tick. */
    private boolean reboteAplicadoEsteTick = false;

    /** Marca de tiempo (ms desde epoch) para la proxima aparicion espontanea de OVNI. */
    private long tiempoProximoOvniMs;

    /** Aleatorio interno usado para programar y direccionar el OVNI espontaneo. */
    private final Random aleatorioOvni = new Random();

    /** Puntos base por defecto cuando el OVNI aparece de manera espontanea. */
    private static final int PUNTOS_BASE_OVNI_DEFAULT = 1500;

    /** Construye el motor e inicializa la primera oleada. */
    public MotorJuego() {
        synchronized (lock) {
            gestorOleadas.prepararInicio(estado);
            programarSiguienteOvni();
        }
        LoggerUtil.info("motor inicializado (oleada=" + estado.oleada
            + ", bunkers=" + estado.bunkers.tamano()
            + ", aliens=" + estado.aliens.tamano() + ")");
    }

    // ============================================================
    // API publica de orquestacion (antes en GameManager)
    // ============================================================

    /** @return true si todavía hay cupo para más conexiones (jugador o espectador). */
    public boolean tieneEspacio() {
        synchronized (lock) {
            int totalMax = Config.MAX_JUGADORES
                + Config.MAX_JUGADORES * Config.MAX_ESPECTADORES_POR_JUGADOR;
            return estado.jugadores.tamano() + estado.espectadores.size() < totalMax;
        }
    }

    /** @return true si hay al menos un jugador con vidas {@code > 0}. */
    public boolean hayJugadorActivo() {
        synchronized (lock) {
            if (estado.jugadores.estaVacia()) return false;
            for (Jugador j : estado.jugadores) {
                if (j.estaVivo()) return true;
            }
            return false;
        }
    }

    /** @return cantidad de jugadores con vidas {@code > 0}. */
    public int contarJugadoresActivos() {
        synchronized (lock) {
            int n = 0;
            for (Jugador j : estado.jugadores) {
                if (j.estaVivo()) n++;
            }
            return n;
        }
    }

    /**
     * Registra un jugador nuevo en la partida y crea su cañón.
     *
     * @param id id del jugador.
     * @param x  posición X inicial del cañón.
     * @param y  posición Y inicial del cañón.
     * @return true si fue agregado, false si la sala está llena.
     */
    public boolean agregarJugador(String id, double x, double y) {
        synchronized (lock) {
            if (estado.jugadores.tamano() >= Config.MAX_JUGADORES) {
                LoggerUtil.warning("agregarJugador rechazado: limite alcanzado (" + id + ")");
                return false;
            }
            boolean primerJugador = estado.jugadores.estaVacia();
            Jugador jugador = new Jugador(id, id);
            Canon canon = new Canon(GeneradorIds.siguiente("C"), id, x, y);
            jugador.asignarCanon(canon);
            estado.jugadores.agregar(jugador);
            estado.canones.agregar(canon);
            /* Si es el primer jugador en entrar, el motor estuvo en pausa
             * (ver tick). Reprogramamos el proximo OVNI desde "ahora"
             * para que no spawnee al instante por timer acumulado. */
            if (primerJugador) {
                programarSiguienteOvni();
            }
            LoggerUtil.info("jugador agregado: " + id);
            notificar(estado.aMapa());
            return true;
        }
    }

    /**
     * Registra un espectador asociado a un jugador especifico.
     *
     * <p>Cada espectador debe declarar (en su mensaje CONNECT) cual jugador
     * piensa observar. El motor valida que ese jugador exista antes de
     * aceptar la conexion, ya que el enunciado (RF-CE03) indica que "cada
     * jugador tiene su propio espectador asociado" — no se permiten
     * espectadores "globales" sin objetivo.</p>
     *
     * <p>Reglas aplicadas en orden:</p>
     * <ol>
     *   <li>Si {@code idJugadorObservado} es {@code null}, se rechaza.</li>
     *   <li>Si no existe un jugador registrado con ese id, se rechaza.</li>
     *   <li>Si el cupo global de espectadores esta lleno, se rechaza.</li>
     *   <li>Si el espectador ya estaba registrado, se trata como caso
     *       idempotente (retorna {@code true} sin duplicar).</li>
     *   <li>Caso exito: se agrega al set y se trackea la relacion en
     *       {@link EstadoJuego#espectadoresPorJugador}.</li>
     * </ol>
     *
     * @param idEspectador       id del espectador que se quiere registrar.
     * @param idJugadorObservado id del jugador que el espectador desea ver.
     * @return true si fue aceptado, false en caso contrario.
     */
    public boolean registrarEspectador(String idEspectador, String idJugadorObservado) {
        synchronized (lock) {
            if (idJugadorObservado == null) {
                LoggerUtil.warning("registrarEspectador rechazado: target nulo (" + idEspectador + ")");
                return false;
            }
            // Validar que el jugador objetivo exista.
            Jugador objetivo = null;
            for (Jugador j : estado.jugadores) {
                if (idJugadorObservado.equals(j.getId())) {
                    objetivo = j;
                    break;
                }
            }
            if (objetivo == null) {
                LoggerUtil.warning("registrarEspectador rechazado: jugador target inexistente ("
                    + idEspectador + " -> " + idJugadorObservado + ")");
                return false;
            }
            // Validar cupo global.
            int activos = 0;
            for (Jugador j : estado.jugadores) {
                if (j.estaVivo()) activos++;
            }
            int cupoMax = Math.max(activos, 1) * Config.MAX_ESPECTADORES_POR_JUGADOR;
            if (estado.espectadores.size() >= cupoMax
                && !estado.espectadores.contains(idEspectador)) {
                LoggerUtil.warning("registrarEspectador rechazado: cupo lleno (" + idEspectador + ")");
                return false;
            }
            // Idempotencia: si ya estaba registrado, no duplicar.
            if (estado.espectadores.contains(idEspectador)) {
                LoggerUtil.info("registrarEspectador idempotente: " + idEspectador
                    + " ya estaba registrado");
                // Reafirmamos el mapeo por si cambio el target.
                estado.espectadoresPorJugador.put(idEspectador, idJugadorObservado);
                return true;
            }
            estado.espectadores.add(idEspectador);
            estado.espectadoresPorJugador.put(idEspectador, idJugadorObservado);
            LoggerUtil.info("espectador " + idEspectador + " observando a " + idJugadorObservado);
            return true;
        }
    }

    /** Elimina al jugador y libera su cañón. */
    public void eliminarJugador(String id) {
        synchronized (lock) {
            Jugador encontrado = null;
            for (Jugador j : estado.jugadores) {
                if (id.equals(j.getId())) {
                    encontrado = j;
                    break;
                }
            }
            if (encontrado == null) return;
            estado.jugadores.eliminar(encontrado);

            Canon canonAEliminar = null;
            for (Canon c : estado.canones) {
                if (id.equals(c.getJugadorId())) {
                    canonAEliminar = c;
                    break;
                }
            }
            if (canonAEliminar != null) {
                estado.canones.eliminar(canonAEliminar);
            }
            LoggerUtil.info("jugador eliminado: " + id);
            notificar(estado.aMapa());
        }
    }

    /** Quita un espectador del registro y libera su mapeo con el jugador observado. */
    public void eliminarEspectador(String id) {
        synchronized (lock) {
            boolean estaba = estado.espectadores.remove(id);
            estado.espectadoresPorJugador.remove(id);
            if (estaba) {
                LoggerUtil.info("espectador eliminado: " + id);
            }
        }
    }

    /**
     * Aplica una acción del protocolo enviada por un jugador.
     *
     * @param id     id del jugador.
     * @param accion {@code MOVE_LEFT}, {@code MOVE_RIGHT}, {@code FIRE} o {@code STOP}.
     */
    public void procesarInput(String id, String accion) {
        synchronized (lock) {
            procesarAccion(id, accion);
            notificar(estado.aMapa());
        }
    }

    /**
     * Snapshot del estado actual del juego en formato {@link Map}.
     *
     * @return mapa serializable (no es el original; es seguro modificarlo).
     */
    public Map<String, Object> getEstadoJuego() {
        synchronized (lock) {
            return estado.aMapa();
        }
    }

    /**
     * Ejecuta un tick completo del motor.
     *
     * <p>Después de aplicar las reglas, primero notifica los eventos puntuales
     * en el orden en que se generaron, y luego envía el snapshot actualizado.</p>
     *
     * @param deltaTime tiempo (segundos) desde el tick anterior. Actualmente
     *                  no se usa: el motor avanza en pasos discretos. Se conserva
     *                  el parámetro para que la integración con GameLoop sea
     *                  directa en Fase 2.
     */
    public void actualizar(double deltaTime) {
        synchronized (lock) {
            List<EventoJuego> buffer = new ArrayList<>();
            DetectorColisiones.SinkEventos sink = buffer::add;
            tick(sink);
            for (EventoJuego e : buffer) {
                notificar(e);
            }
            notificar(estado.aMapa());
        }
    }

    // ============================================================
    // API administrativa (comandos del enunciado, invocados por la
    // ConsolaAdmin). Cada metodo corre bajo el lock global y emite
    // las notificaciones correspondientes a los observadores.
    // ============================================================

    /**
     * Comando admin: {@code Crear (X, Y, Pts)}. Spawnea un alien adicional
     * en la posicion indicada con el puntaje solicitado (10, 20 o 40).
     *
     * <p>Si el puntaje no corresponde a un tipo valido se rechaza el comando
     * y se loggea una advertencia; el estado no cambia.</p>
     *
     * @param x      posicion X del nuevo alien.
     * @param y      posicion Y del nuevo alien.
     * @param puntos puntaje deseado (10=Squid, 20=Crab, 40=Octopus).
     */
    public void crearAlienAdmin(double x, double y, int puntos) {
        synchronized (lock) {
            try {
                Alien a = fabricaConcreta.crearPorPuntos(x, y, puntos);
                estado.aliens.agregar(a);
                LoggerUtil.info("admin: alien creado en (" + x + "," + y + ") puntos=" + puntos);
                notificar(estado.aMapa());
            } catch (IllegalArgumentException ex) {
                LoggerUtil.warning("admin: rechazado crearAlien: " + ex.getMessage());
            }
        }
    }

    /**
     * Comando admin: {@code OVNI I-D <pts>} / {@code OVNI D-I <pts>}.
     * Spawnea un OVNI con la direccion y los puntos base indicados.
     *
     * <p>Si ya hay un OVNI activo, ignora la solicitud (no sobreescribe el
     * OVNI vivo) y loggea una advertencia. Tras crear el OVNI se reprograma
     * la siguiente aparicion espontanea para evitar que se solape de
     * inmediato.</p>
     *
     * @param direccion  direccion horizontal del OVNI.
     * @param puntosBase puntos base que se otorgaran al derribarlo.
     */
    public void crearOvniAdmin(Ovni.Direccion direccion, int puntosBase) {
        synchronized (lock) {
            if (estado.ovni != null && estado.ovni.estaVivo()) {
                LoggerUtil.warning("admin: ya hay un OVNI activo, ignorando");
                return;
            }
            double x = (direccion == Ovni.Direccion.IZQUIERDA_A_DERECHA)
                ? 0.0
                : (Config.CAMPO_ANCHO - Config.ANCHO_OVNI);
            double y = 30.0;
            estado.ovni = fabricaConcreta.crearOvni(x, y, direccion, puntosBase);
            programarSiguienteOvni();
            LoggerUtil.info("admin: OVNI creado dir=" + direccion + " puntosBase=" + puntosBase);
            notificar(estado.aMapa());
        }
    }

    /**
     * Comando admin: {@code Velocidad <ms>}. Cambia el intervalo (ms) entre
     * pasos del bloque de aliens. Se aplica clamp al minimo definido en
     * {@link Config#ALIENS_INTERVALO_MIN_MS} para evitar valores sin sentido.
     *
     * <p>Emite primero un evento {@link EventoJuego.TipoEvento#SPEED_CHANGED}
     * con el nuevo intervalo y luego el snapshot completo del estado.</p>
     *
     * @param intervaloMs intervalo solicitado en milisegundos.
     */
    public void setVelocidadAliens(long intervaloMs) {
        synchronized (lock) {
            long clamped = Math.max(Config.ALIENS_INTERVALO_MIN_MS, intervaloMs);
            estado.intervaloAliensMs = clamped;
            LoggerUtil.info("admin: velocidad aliens = " + clamped + " ms");
            notificar(new EventoJuego(EventoJuego.TipoEvento.SPEED_CHANGED,
                java.util.Map.of("intervaloMs", clamped)));
            notificar(estado.aMapa());
        }
    }

    /**
     * Comando admin: {@code Bunkers <pct>%}. Fija el porcentaje de salud de
     * <strong>todos</strong> los bunkers a {@code pct}, con clamp a [0, 100].
     * Si el valor es 0 los bunkers quedan destruidos; si es mayor a 0 se
     * reviven automaticamente (ver {@link Bunker#fijarSaludAdmin(int)}).
     *
     * @param pct porcentaje de salud deseado (sera limitado a 0..100).
     */
    public void setSaludBunkers(int pct) {
        synchronized (lock) {
            int clamped = Math.max(0, Math.min(100, pct));
            for (Bunker b : estado.bunkers) {
                b.fijarSaludAdmin(clamped);
            }
            LoggerUtil.info("admin: salud bunkers = " + clamped + "%");
            notificar(estado.aMapa());
        }
    }

    /** Cierra el motor (placeholder por simetría con la red). */
    public void shutdown() {
        synchronized (lock) {
            LoggerUtil.info("motor cerrado");
        }
    }

    // ============================================================
    // Accesores package-private para pruebas unitarias
    // ============================================================

    /**
     * Acceso al estado interno (package-private, solo para tests).
     * @return referencia directa al {@link EstadoJuego} interno.
     */
    EstadoJuego getEstadoInterno() {
        return estado;
    }

    /**
     * Ejecuta un tick sobre el estado interno emitiendo eventos al sink dado
     * (package-private, solo para tests).
     */
    void tickInterno(DetectorColisiones.SinkEventos sink) {
        tick(sink);
    }

    /**
     * Ejecuta una accion sin tomar el lock global ni notificar
     * (package-private, solo para tests).
     */
    void procesarAccionInterno(String jugadorId, String accion) {
        procesarAccion(jugadorId, accion);
    }

    // ============================================================
    // Logica interna del tick (antes en MotorJuego antiguo)
    // ============================================================

    /**
     * Avanza un tick del motor sobre el estado interno.
     *
     * @param sink destino de eventos generados durante el tick.
     */
    private void tick(DetectorColisiones.SinkEventos sink) {
        if (estado.juegoTerminado) return;

        /* Si todavia no hay jugadores conectados, el motor se queda en
         * pausa: no mueve aliens, no spawnea OVNI, no avanza nada. De lo
         * contrario los aliens descienden mientras el primer cliente
         * esta en la pantalla de inicio y, al conectar, ya estarian fuera
         * del campo visible. Es el "ready state" del juego: el motor
         * arranca cuando alguien entra a jugar. */
        if (estado.jugadores.estaVacia()) return;

        verificarSpawnOvni(estado);
        moverEntidades();
        manejarBordesAliens();
        dispararAliensAleatorio();
        balasFueraDePantalla();
        detector.detectar(estado, sink);
        limpiarMuertos();

        if (estado.aliens.estaVacia() && !estado.juegoTerminado) {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("oleada", estado.oleada);
            sink.emitir(new EventoJuego(EventoJuego.TipoEvento.WAVE_CLEARED, payload));
            gestorOleadas.siguienteOleada(estado, sink);
        }
    }

    /**
     * Procesa una acción del protocolo enviada por un jugador.
     *
     * @param jugadorId id del jugador autor de la acción.
     * @param accion    acción del protocolo ({@code MOVE_LEFT}, {@code MOVE_RIGHT},
     *                  {@code FIRE}, {@code STOP}).
     */
    private void procesarAccion(String jugadorId, String accion) {
        Canon canon = buscarCanonDe(jugadorId);
        if (canon == null) return;
        switch (accion) {
            case "MOVE_LEFT" -> canon.moverIzquierda();
            case "MOVE_RIGHT" -> canon.moverDerecha();
            case "FIRE" -> dispararJugador(jugadorId, canon);
            case "STOP" -> { /* no-op: solo deja de moverse */ }
            default -> LoggerUtil.warning("accion desconocida: " + accion);
        }
    }

    /** Mueve aliens, balas y OVNI un tick. */
    private void moverEntidades() {
        reboteAplicadoEsteTick = false;
        for (Alien a : estado.aliens) {
            if (a.estaVivo()) a.mover();
        }
        for (Bala b : estado.balas) {
            if (b.estaVivo()) b.mover();
        }
        if (estado.ovni != null && estado.ovni.estaVivo()) {
            estado.ovni.mover();
            double xOvni = estado.ovni.getX();
            if (xOvni + Config.ANCHO_OVNI < 0 || xOvni > Config.CAMPO_ANCHO) {
                estado.ovni.destruir();
            }
        }
    }

    /** Si algún alien tocó borde lateral, todos invierten y bajan. */
    private void manejarBordesAliens() {
        if (reboteAplicadoEsteTick) return;
        boolean tocoBorde = false;
        for (Alien a : estado.aliens) {
            if (!a.estaVivo()) continue;
            if (a.getX() <= 0 || a.getX() + a.getAncho() >= Config.CAMPO_ANCHO) {
                tocoBorde = true;
                break;
            }
        }
        if (!tocoBorde) return;
        for (Alien a : estado.aliens) {
            if (!a.estaVivo()) continue;
            a.invertirDireccion();
            a.bajarFila();
        }
        reboteAplicadoEsteTick = true;
    }

    /**
     * Lanza una bala desde un alien aleatorio (de la fila inferior por columna).
     * La probabilidad por tick es {@link Config#PROB_DISPARO_ALIEN_POR_TICK}.
     */
    private void dispararAliensAleatorio() {
        if (estado.aliens.estaVacia()) return;
        if (aleatorio.nextDouble() >= Config.PROB_DISPARO_ALIEN_POR_TICK) return;

        // Toma los aliens de la fila inferior por columna (el de mayor Y para cada X).
        List<Alien> candidatos = new ArrayList<>();
        for (Alien a : estado.aliens) {
            if (!a.estaVivo()) continue;
            boolean esInferior = true;
            for (Alien otro : estado.aliens) {
                if (otro == a || !otro.estaVivo()) continue;
                // misma columna aproximada (X dentro del ancho) y otro está debajo
                if (Math.abs(otro.getX() - a.getX()) < a.getAncho() / 2.0
                    && otro.getY() > a.getY()) {
                    esInferior = false;
                    break;
                }
            }
            if (esInferior) candidatos.add(a);
        }
        if (candidatos.isEmpty()) return;

        Alien tirador = candidatos.get(aleatorio.nextInt(candidatos.size()));
        double bx = tirador.getX() + (tirador.getAncho() - Config.ANCHO_BALA) / 2.0;
        double by = tirador.getY() + tirador.getAlto();
        Bala bala = new Bala(GeneradorIds.siguiente("B"), bx, by,
            Bala.Direccion.ABAJO, Bala.DUENIO_ALIEN);
        estado.balas.agregarAlInicio(bala);
    }

    /** Marca como destruidas las balas que salieron del campo. */
    private void balasFueraDePantalla() {
        for (Bala b : estado.balas) {
            if (!b.estaVivo()) continue;
            if (b.getY() + b.getAlto() < 0 || b.getY() > Config.CAMPO_ALTO) {
                b.destruir();
            }
        }
    }

    /**
     * Elimina entidades marcadas como no-vivas. Usa el patrón
     * "recolectar y eliminar después" para no romper la iteración.
     */
    private void limpiarMuertos() {
        List<Bala> balasMuertas = new ArrayList<>();
        for (Bala b : estado.balas) {
            if (!b.estaVivo()) balasMuertas.add(b);
        }
        for (Bala b : balasMuertas) {
            estado.balas.eliminar(b);
        }

        List<Alien> aliensMuertos = new ArrayList<>();
        for (Alien a : estado.aliens) {
            if (!a.estaVivo()) aliensMuertos.add(a);
        }
        for (Alien a : aliensMuertos) {
            estado.aliens.eliminar(a);
        }

        // Bunkers destruidos: se mantienen en la lista pero se filtran en aMapa.
        // OVNI muerto: se libera la referencia.
        if (estado.ovni != null && !estado.ovni.estaVivo()) {
            estado.ovni = null;
        }
    }

    /**
     * Reprograma la marca de tiempo para la siguiente aparicion espontanea
     * del OVNI, eligiendo un retraso uniforme en el intervalo
     * {@code [OVNI_INTERVALO_MIN_SEG, OVNI_INTERVALO_MAX_SEG]} segundos.
     */
    private void programarSiguienteOvni() {
        double rango = Config.OVNI_INTERVALO_MAX_SEG - Config.OVNI_INTERVALO_MIN_SEG;
        double segundos = Config.OVNI_INTERVALO_MIN_SEG + aleatorioOvni.nextDouble() * rango;
        tiempoProximoOvniMs = System.currentTimeMillis() + (long) (segundos * 1000);
    }

    /**
     * Verifica si llego el momento de spawnear un OVNI espontaneo y, de ser
     * asi, lo crea con direccion aleatoria y puntos base
     * {@link #PUNTOS_BASE_OVNI_DEFAULT}. No spawnea si ya hay un OVNI vivo.
     */
    private void verificarSpawnOvni(EstadoJuego e) {
        if (e.ovni != null && e.ovni.estaVivo()) return;
        if (System.currentTimeMillis() < tiempoProximoOvniMs) return;
        Ovni.Direccion dir = aleatorioOvni.nextBoolean()
            ? Ovni.Direccion.IZQUIERDA_A_DERECHA
            : Ovni.Direccion.DERECHA_A_IZQUIERDA;
        double x = (dir == Ovni.Direccion.IZQUIERDA_A_DERECHA)
            ? 0.0
            : (Config.CAMPO_ANCHO - Config.ANCHO_OVNI);
        double y = 30.0;
        e.ovni = fabricaConcreta.crearOvni(x, y, dir, PUNTOS_BASE_OVNI_DEFAULT);
        programarSiguienteOvni();
        LoggerUtil.debug("ovni espontaneo dir=" + dir);
    }

    private Canon buscarCanonDe(String jugadorId) {
        for (Canon c : estado.canones) {
            if (jugadorId.equals(c.getJugadorId())) return c;
        }
        return null;
    }

    /**
     * Dispara una bala del jugador si no excede {@link Config#MAX_BALAS_JUGADOR}.
     */
    private void dispararJugador(String jugadorId, Canon canon) {
        int balasDelJugador = 0;
        for (Bala b : estado.balas) {
            if (b.estaVivo() && jugadorId.equals(b.getDuenioId())) {
                balasDelJugador++;
            }
        }
        if (balasDelJugador >= Config.MAX_BALAS_JUGADOR) return;
        estado.balas.agregarAlInicio(canon.disparar(GeneradorIds.siguiente("B")));
    }
}
