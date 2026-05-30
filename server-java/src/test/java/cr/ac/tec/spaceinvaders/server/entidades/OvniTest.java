package cr.ac.tec.spaceinvaders.server.entidades;

import cr.ac.tec.spaceinvaders.server.util.Config;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas para {@link Ovni}.
 */
class OvniTest {

    @Test
    @DisplayName("mover IZQUIERDA_A_DERECHA aumenta X")
    void moverIzquierdaADerechaSumaX() {
        Ovni o = new Ovni("ovni", 100, 50, Ovni.Direccion.IZQUIERDA_A_DERECHA, 1000);
        o.mover();
        assertEquals(100 + Config.OVNI_VELOCIDAD, o.getX(), 1e-9);
    }

    @Test
    @DisplayName("mover DERECHA_A_IZQUIERDA disminuye X")
    void moverDerechaAIzquierdaRestaX() {
        Ovni o = new Ovni("ovni", 100, 50, Ovni.Direccion.DERECHA_A_IZQUIERDA, 1000);
        o.mover();
        assertEquals(100 - Config.OVNI_VELOCIDAD, o.getX(), 1e-9);
    }

    @Test
    @DisplayName("otorgarPuntosAleatorios cae en [base*MIN, base*MAX] con semilla fija")
    void otorgarPuntosDentroDeRangoConSemillaFija() {
        int base = 1500;
        Random semilla = new Random(42L);
        Ovni o = new Ovni("ovni", 0, 0, Ovni.Direccion.IZQUIERDA_A_DERECHA, base, semilla);

        int puntos = o.otorgarPuntosAleatorios();
        int min = (int) Math.round(base * Config.OVNI_PUNTOS_MULT_MIN);
        int max = (int) Math.round(base * Config.OVNI_PUNTOS_MULT_MAX);
        assertTrue(puntos >= min, "puntos=" + puntos + " debe ser >= " + min);
        assertTrue(puntos <= max, "puntos=" + puntos + " debe ser <= " + max);
    }

    @Test
    @DisplayName("otorgarPuntosAleatorios es reproducible con la misma semilla")
    void otorgarPuntosReproducible() {
        int base = 1500;
        Ovni a = new Ovni("a", 0, 0, Ovni.Direccion.IZQUIERDA_A_DERECHA, base, new Random(123L));
        Ovni b = new Ovni("b", 0, 0, Ovni.Direccion.IZQUIERDA_A_DERECHA, base, new Random(123L));
        assertEquals(a.otorgarPuntosAleatorios(), b.otorgarPuntosAleatorios());
    }

    @Test
    @DisplayName("getters básicos exponen estado configurado")
    void gettersExponenEstado() {
        Ovni o = new Ovni("ovni", 10, 20, Ovni.Direccion.DERECHA_A_IZQUIERDA, 800);
        assertEquals(Ovni.Direccion.DERECHA_A_IZQUIERDA, o.getDireccion());
        assertEquals(800, o.getPuntosBase());
        assertEquals(Config.ANCHO_OVNI, o.getAncho());
        assertEquals(Config.ALTO_OVNI, o.getAlto());
    }
}
