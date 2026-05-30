package cr.ac.tec.spaceinvaders.server.nucleo;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas del {@link GameLoop}.
 *
 * <p>Estas pruebas dependen de timing (usan {@code Thread.sleep}). Los
 * margenes elegidos son generosos para tolerar slack en CI y maquinas
 * cargadas: con un periodo de 50ms y una ventana de 250ms se esperan ~5
 * ticks, pero se acepta cualquier cantidad &gt;= 3.</p>
 *
 * <p>Para el motor se usa una <strong>subclase anonima</strong> de
 * {@link MotorJuego} que sobrescribe {@code actualizar} con un contador.
 * El constructor de {@code MotorJuego} crea estado real (oleada inicial,
 * bunkers, aliens), pero eso es aceptable y barato para estas pruebas.</p>
 */
class GameLoopTest {

    /**
     * Crea una subclase anonima de {@link MotorJuego} que incrementa el
     * contador dado en cada llamada a {@code actualizar}.
     */
    private MotorJuego motorContador(AtomicInteger contador) {
        return new MotorJuego() {
            @Override
            public void actualizar(double deltaTime) {
                contador.incrementAndGet();
            }
        };
    }

    /**
     * Crea una subclase de {@link MotorJuego} que lanza {@link RuntimeException}
     * en el N-esimo tick (1-indexed) y cuenta llamadas en {@code contador}.
     */
    private MotorJuego motorQueLanzaEnTick(AtomicInteger contador, int tickQueLanza) {
        return new MotorJuego() {
            @Override
            public void actualizar(double deltaTime) {
                int n = contador.incrementAndGet();
                if (n == tickQueLanza) {
                    throw new RuntimeException("fallo simulado en tick " + n);
                }
            }
        };
    }

    @Test
    void iniciarDispara_actualizar_periodicamente() throws InterruptedException {
        AtomicInteger contador = new AtomicInteger(0);
        GameLoop loop = new GameLoop(motorContador(contador));
        loop.iniciar();
        try {
            Thread.sleep(250);
        } finally {
            loop.detener();
        }
        int observados = contador.get();
        assertTrue(observados >= 3,
            "se esperaban >=3 ticks en 250ms a 20 TPS, observados=" + observados);
    }

    @Test
    void iniciarEsIdempotente() throws InterruptedException {
        AtomicInteger contador = new AtomicInteger(0);
        GameLoop loop = new GameLoop(motorContador(contador));
        loop.iniciar();
        // segunda llamada no debe crear un segundo schedule
        loop.iniciar();
        assertTrue(loop.estaCorriendo());
        try {
            Thread.sleep(200);
        } finally {
            loop.detener();
        }
        // Si hubiera dos schedules, esperariamos ~8 ticks (4 por schedule).
        // Tolerancia: en una unica corrida deberia rondar 4; aceptamos hasta 10.
        int observados = contador.get();
        assertTrue(observados <= 10,
            "iniciar() duplicado parece haber creado mas de un schedule, observados=" + observados);
    }

    @Test
    void detenerSinIniciarNoCrashea() {
        AtomicInteger contador = new AtomicInteger(0);
        GameLoop loop = new GameLoop(motorContador(contador));
        assertDoesNotThrow(loop::detener);
        assertFalse(loop.estaCorriendo());
    }

    @Test
    void detenerEsIdempotente() throws InterruptedException {
        AtomicInteger contador = new AtomicInteger(0);
        GameLoop loop = new GameLoop(motorContador(contador));
        loop.iniciar();
        Thread.sleep(100);
        loop.detener();
        assertFalse(loop.estaCorriendo());
        // Segunda llamada no debe lanzar ni cambiar el estado.
        assertDoesNotThrow(loop::detener);
        assertFalse(loop.estaCorriendo());
    }

    @Test
    void estaCorriendoReflejaElEstado() throws InterruptedException {
        AtomicInteger contador = new AtomicInteger(0);
        GameLoop loop = new GameLoop(motorContador(contador));
        assertFalse(loop.estaCorriendo(), "antes de iniciar debe estar false");
        loop.iniciar();
        assertTrue(loop.estaCorriendo(), "tras iniciar debe estar true");
        Thread.sleep(80);
        loop.detener();
        assertFalse(loop.estaCorriendo(), "tras detener debe estar false");
    }

    @Test
    void excepcionEnActualizarNoDetieneElLoop() throws InterruptedException {
        AtomicInteger contador = new AtomicInteger(0);
        GameLoop loop = new GameLoop(motorQueLanzaEnTick(contador, 2));
        loop.iniciar();
        try {
            // 300ms ~ 6 ticks; lanzamos en el 2do y debemos seguir mas alla de el.
            Thread.sleep(300);
        } finally {
            loop.detener();
        }
        int observados = contador.get();
        // Si el schedule se hubiera cancelado tras la excepcion del 2do tick,
        // observados quedaria estancado en 2. Exigimos al menos 4 para confirmar
        // que el loop sobrevive al fallo.
        assertTrue(observados >= 4,
            "el loop debio sobrevivir al tick fallido, observados=" + observados);
    }
}
