package cr.ac.tec.spaceinvaders.server.cli;

import cr.ac.tec.spaceinvaders.server.entidades.Bunker;
import cr.ac.tec.spaceinvaders.server.entidades.Ovni;
import cr.ac.tec.spaceinvaders.server.nucleo.EstadoJuego;
import cr.ac.tec.spaceinvaders.server.nucleo.MotorJuego;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Pruebas de {@link ConsolaAdmin}, enfocadas en el metodo package-private
 * {@code procesarLinea} para no depender de stdin real.
 *
 * <p>Cada test crea un {@link MotorJuego} fresco y le envia comandos
 * directamente.</p>
 */
class ConsolaAdminTest {

    /**
     * Accede al estado interno del motor via reflexion, replicando la
     * misma facilidad que ya usan otros tests del paquete {@code nucleo}.
     */
    private EstadoJuego estadoDe(MotorJuego motor) {
        try {
            Field f = MotorJuego.class.getDeclaredField("estado");
            f.setAccessible(true);
            return (EstadoJuego) f.get(motor);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    // ============================================================
    // Crear (X, Y, Pts)
    // ============================================================

    @Test
    void crearCrabAgregaUnAlien() {
        MotorJuego motor = new MotorJuego();
        ConsolaAdmin consola = new ConsolaAdmin(motor);
        EstadoJuego e = estadoDe(motor);
        int previo = e.aliens.tamano();

        consola.procesarLinea("Crear (100, 200, 20)");

        assertEquals(previo + 1, e.aliens.tamano());
    }

    @Test
    void crearOctopusConEspaciosExtras() {
        MotorJuego motor = new MotorJuego();
        ConsolaAdmin consola = new ConsolaAdmin(motor);
        EstadoJuego e = estadoDe(motor);
        int previo = e.aliens.tamano();

        consola.procesarLinea("Crear ( 50 , 60 , 40 )");

        assertEquals(previo + 1, e.aliens.tamano());
    }

    @Test
    void crearConPuntosInvalidosNoCrashea() {
        MotorJuego motor = new MotorJuego();
        ConsolaAdmin consola = new ConsolaAdmin(motor);
        EstadoJuego e = estadoDe(motor);
        int previo = e.aliens.tamano();

        assertDoesNotThrow(() -> consola.procesarLinea("Crear (10, 10, 7)"));
        assertEquals(previo, e.aliens.tamano(), "puntos invalidos no agregan alien");
    }

    // ============================================================
    // OVNI I-D <pts> / OVNI D-I <pts>
    // ============================================================

    @Test
    void ovniIDCreaOvniHaciaDerecha() {
        MotorJuego motor = new MotorJuego();
        ConsolaAdmin consola = new ConsolaAdmin(motor);
        EstadoJuego e = estadoDe(motor);
        e.ovni = null;

        consola.procesarLinea("OVNI I-D 1500");

        assertNotNull(e.ovni);
        assertEquals(Ovni.Direccion.IZQUIERDA_A_DERECHA, e.ovni.getDireccion());
        assertEquals(1500, e.ovni.getPuntosBase());
    }

    @Test
    void ovniDICreaOvniHaciaIzquierda() {
        MotorJuego motor = new MotorJuego();
        ConsolaAdmin consola = new ConsolaAdmin(motor);
        EstadoJuego e = estadoDe(motor);
        e.ovni = null;

        consola.procesarLinea("OVNI D-I 2000");

        assertNotNull(e.ovni);
        assertEquals(Ovni.Direccion.DERECHA_A_IZQUIERDA, e.ovni.getDireccion());
        assertEquals(2000, e.ovni.getPuntosBase());
    }

    @Test
    void segundoOvniNoSobreescribeAlPrimero() {
        MotorJuego motor = new MotorJuego();
        ConsolaAdmin consola = new ConsolaAdmin(motor);
        EstadoJuego e = estadoDe(motor);
        e.ovni = null;

        consola.procesarLinea("OVNI I-D 1500");
        Ovni primero = e.ovni;
        assertNotNull(primero);

        consola.procesarLinea("OVNI D-I 2000");
        assertSame(primero, e.ovni, "segundo OVNI debe ser ignorado");
    }

    // ============================================================
    // Velocidad <ms>
    // ============================================================

    @Test
    void velocidadCambiaIntervalo() {
        MotorJuego motor = new MotorJuego();
        ConsolaAdmin consola = new ConsolaAdmin(motor);
        EstadoJuego e = estadoDe(motor);

        consola.procesarLinea("Velocidad 100");

        assertEquals(100, e.intervaloAliensMs);
    }

    // ============================================================
    // Bunkers <pct>%
    // ============================================================

    @Test
    void bunkersConPorcentajeFijaSalud() {
        MotorJuego motor = new MotorJuego();
        ConsolaAdmin consola = new ConsolaAdmin(motor);
        EstadoJuego e = estadoDe(motor);

        consola.procesarLinea("Bunkers 70%");

        for (Bunker b : e.bunkers) {
            assertEquals(70, b.getSalud());
        }
    }

    @Test
    void bunkersSinPorcentajeTambienFunciona() {
        MotorJuego motor = new MotorJuego();
        ConsolaAdmin consola = new ConsolaAdmin(motor);
        EstadoJuego e = estadoDe(motor);

        consola.procesarLinea("Bunkers 70");

        for (Bunker b : e.bunkers) {
            assertEquals(70, b.getSalud());
        }
    }

    // ============================================================
    // Manejo de entradas raras / metadatos
    // ============================================================

    @Test
    void comandoDesconocidoNoCrashea() {
        MotorJuego motor = new MotorJuego();
        ConsolaAdmin consola = new ConsolaAdmin(motor);

        assertDoesNotThrow(() -> consola.procesarLinea("comando random"));
    }

    @Test
    void ayudaNoCrashea() {
        MotorJuego motor = new MotorJuego();
        ConsolaAdmin consola = new ConsolaAdmin(motor);

        assertDoesNotThrow(() -> consola.procesarLinea("ayuda"));
        assertDoesNotThrow(() -> consola.procesarLinea("help"));
    }

    @Test
    void lineaVaciaONullNoCrashea() {
        MotorJuego motor = new MotorJuego();
        ConsolaAdmin consola = new ConsolaAdmin(motor);

        assertDoesNotThrow(() -> consola.procesarLinea(""));
        assertDoesNotThrow(() -> consola.procesarLinea("   "));
        assertDoesNotThrow(() -> consola.procesarLinea(null));
    }

    @Test
    void salirNoCrashea() {
        MotorJuego motor = new MotorJuego();
        ConsolaAdmin consola = new ConsolaAdmin(motor);

        assertDoesNotThrow(() -> consola.procesarLinea("salir"));
    }
}
