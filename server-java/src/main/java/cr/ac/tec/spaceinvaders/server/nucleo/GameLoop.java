package cr.ac.tec.spaceinvaders.server.nucleo;

import cr.ac.tec.spaceinvaders.server.util.Config;
import cr.ac.tec.spaceinvaders.server.util.LoggerUtil;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * Hilo dedicado que ejecuta el tick del juego a {@link Config#TICKS_POR_SEGUNDO}
 * ticks por segundo invocando {@link MotorJuego#actualizar(double)}.
 *
 * <p><strong>Responsabilidad.</strong> Desacopla el avance temporal del motor
 * de la capa de red y de la CLI administrativa. El servidor TCP solo procesa
 * inputs entrantes; este loop garantiza que el estado del juego siga avanzando
 * (movimiento de aliens, balas, OVNI, transicion de oleadas, etc.) aun cuando
 * no haya inputs de los clientes.</p>
 *
 * <p><strong>Por que {@link ScheduledExecutorService} y no {@code Thread.sleep}.</strong>
 * Un {@code while(true){ tick(); Thread.sleep(50); }} tiene dos problemas
 * dificiles de manejar a mano:</p>
 * <ul>
 *   <li><em>Drift acumulado</em>: cada iteracion arranca <code>tiempo_del_tick</code>
 *       milisegundos despues del ideal, asi que el ritmo real es siempre menor
 *       a 20 TPS.</li>
 *   <li><em>Manejo de la interrupcion y cancelacion</em>: hay que entrelazar
 *       chequeos de {@code Thread.interrupted()} y catch de
 *       {@link InterruptedException} en cada iteracion.</li>
 * </ul>
 * <p>{@code scheduleAtFixedRate} resuelve ambos: agenda los disparos a un
 * intervalo fijo desde el momento inicial (compensa drift mientras los ticks
 * sean mas rapidos que el periodo) y la cancelacion es declarativa via
 * {@link ScheduledExecutorService#shutdown()}.</p>
 *
 * <p><strong>Advertencia critica.</strong> Si el {@code Runnable} agendado en
 * {@code scheduleAtFixedRate} lanza una excepcion, el executor
 * <em>silenciosamente cancela todos los ticks futuros</em> sin loggear nada.
 * Para evitar que un bug en {@code motor.actualizar} mate el loop, el Runnable
 * esta envuelto en {@code try/catch(Throwable)} que loggea pero nunca propaga.</p>
 *
 * <p><strong>Hilo nombrado.</strong> El executor usa un {@link ThreadFactory}
 * que nombra al hilo {@code "gameloop"} para que aparezca claramente en
 * {@code jstack}, profilers o {@code thread dumps}. Se marca como
 * {@code daemon = false}: el loop puede mantener vivo al proceso por si solo,
 * aunque normalmente {@code Main} usa {@code hiloServidor.join()} para
 * controlar la vida.</p>
 *
 * <p><strong>Ciclo de vida.</strong> {@link #iniciar()} y {@link #detener()}
 * son idempotentes; llamar dos veces seguidas (o detener sin haber iniciado)
 * solo loggea una advertencia y no causa efectos. {@link #estaCorriendo()}
 * refleja el flag en cualquier momento.</p>
 *
 * <p><strong>Concurrencia.</strong> El flag {@code corriendo} se declara
 * {@code volatile} porque solo se modifica desde el hilo que llama a
 * {@code iniciar}/{@code detener} (tipicamente Main o la consola admin); el
 * loop interno no lo escribe. No se requiere {@code synchronized} ni
 * {@link java.util.concurrent.atomic.AtomicBoolean}.</p>
 *
 * @see MotorJuego#actualizar(double)
 * @see Config#TICKS_POR_SEGUNDO
 * @see Config#INTERVALO_TICK_MS
 */
public class GameLoop {

    /** Motor del juego cuyo {@code actualizar} se invoca cada tick. */
    private final MotorJuego motor;

    /** Executor de un solo hilo que dispara los ticks. Se recrea en cada {@link #iniciar()}. */
    private ScheduledExecutorService executor;

    /** Handle del schedule para inspeccion/cancelacion (no estrictamente necesario, pero util). */
    private ScheduledFuture<?> handleTick;

    /** Marca de tiempo (nanos) del tick previo para calcular el delta real. */
    private long lastTickNs;

    /**
     * Estado de "loop activo". Se lee desde {@link #estaCorriendo()} y se
     * mutua solo desde {@link #iniciar()} / {@link #detener()}; el hilo del
     * tick no lo escribe. {@code volatile} alcanza para que lectores en otros
     * hilos vean los cambios sin sincronizacion explicita.
     */
    private volatile boolean corriendo = false;

    /**
     * Construye un {@code GameLoop} asociado al motor dado. No arranca el
     * ticking; para eso hay que llamar {@link #iniciar()}.
     *
     * @param motor motor que recibira las llamadas a {@code actualizar}.
     */
    public GameLoop(MotorJuego motor) {
        this.motor = motor;
    }

    /**
     * Arranca el ticking en un hilo dedicado llamado {@code "gameloop"}.
     *
     * <p>Si ya esta corriendo, no hace nada salvo loggear una advertencia.
     * Crea un nuevo {@link ScheduledExecutorService} (con un solo hilo
     * nombrado) y agenda el tick con {@code scheduleAtFixedRate} cada
     * {@link Config#INTERVALO_TICK_MS} milisegundos a partir de
     * {@code t=0}.</p>
     *
     * <p>Inicializa {@code lastTickNs} con {@link System#nanoTime()}: la
     * primera invocacion al motor usara, por convencion, un delta igual al
     * intervalo nominal ({@link Config#INTERVALO_TICK_MS} ms / 1000) porque
     * todavia no hay tick previo real.</p>
     */
    public void iniciar() {
        if (corriendo) {
            LoggerUtil.warning("gameloop ya esta corriendo: iniciar ignorado");
            return;
        }
        executor = Executors.newSingleThreadScheduledExecutor(crearFabricaHilos());
        lastTickNs = System.nanoTime();
        handleTick = executor.scheduleAtFixedRate(
            this::ejecutarTickSeguro,
            0L,
            Config.INTERVALO_TICK_MS,
            TimeUnit.MILLISECONDS
        );
        corriendo = true;
        LoggerUtil.info("gameloop iniciado a " + Config.TICKS_POR_SEGUNDO + " TPS");
    }

    /**
     * Detiene el loop limpiamente. Si no esta corriendo, loggea una
     * advertencia y retorna.
     *
     * <p>Invoca {@code shutdown()} sobre el executor y espera hasta 2 segundos
     * con {@code awaitTermination}. Si el hilo del tick no responde en ese
     * tiempo (por ejemplo, porque {@code motor.actualizar} esta atascado en un
     * lock), se llama a {@code shutdownNow()} para forzar la interrupcion.</p>
     */
    public void detener() {
        if (!corriendo) {
            LoggerUtil.warning("gameloop no esta corriendo: detener ignorado");
            return;
        }
        corriendo = false;
        try {
            if (handleTick != null) {
                handleTick.cancel(false);
            }
            executor.shutdown();
            if (!executor.awaitTermination(2, TimeUnit.SECONDS)) {
                LoggerUtil.warning("gameloop no termino en 2s, forzando shutdownNow");
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            LoggerUtil.error("interrupcion al detener gameloop: " + e.getMessage());
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        LoggerUtil.info("gameloop detenido");
    }

    /**
     * @return {@code true} si el loop esta agendado y avanzando ticks,
     *         {@code false} en otro caso.
     */
    public boolean estaCorriendo() {
        return corriendo;
    }

    /**
     * Ejecuta un tick aislando cualquier excepcion del motor para que
     * {@code scheduleAtFixedRate} no cancele los disparos posteriores.
     *
     * <p>Calcula el delta en segundos como {@code (ahora - lastTickNs) / 1e9}.
     * Si {@code lastTickNs == 0} (caso defensivo, no deberia darse porque
     * {@link #iniciar()} lo setea) se usa el intervalo nominal.</p>
     */
    private void ejecutarTickSeguro() {
        try {
            long ahora = System.nanoTime();
            double deltaSegundos;
            if (lastTickNs == 0L) {
                deltaSegundos = Config.INTERVALO_TICK_MS / 1000.0;
            } else {
                deltaSegundos = (ahora - lastTickNs) / 1_000_000_000.0;
            }
            lastTickNs = ahora;
            motor.actualizar(deltaSegundos);
        } catch (Throwable e) {
            // Capturamos Throwable (no solo Exception) para que ni siquiera un
            // Error fatal en el tick cancele el schedule del executor.
            LoggerUtil.error("error en tick del juego: " + e.getMessage());
        }
    }

    /**
     * @return fabrica de hilos que produce un unico hilo llamado
     *         {@code "gameloop"}, no-daemon, para que sea facil de identificar
     *         en thread dumps y profilers.
     */
    private ThreadFactory crearFabricaHilos() {
        return runnable -> {
            Thread hilo = new Thread(runnable, "gameloop");
            hilo.setDaemon(false);
            return hilo;
        };
    }
}
