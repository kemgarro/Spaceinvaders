package cr.ac.tec.spaceinvaders.server.nucleo;

import cr.ac.tec.spaceinvaders.server.entidades.Bala;
import cr.ac.tec.spaceinvaders.server.entidades.Bunker;
import cr.ac.tec.spaceinvaders.server.entidades.Canon;
import cr.ac.tec.spaceinvaders.server.entidades.Crab;
import cr.ac.tec.spaceinvaders.server.entidades.Jugador;
import cr.ac.tec.spaceinvaders.server.entidades.Squid;
import cr.ac.tec.spaceinvaders.server.eventos.EventoJuego;
import cr.ac.tec.spaceinvaders.server.util.Config;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DetectorColisionesTest {

    private List<EventoJuego> capturar(DetectorColisiones d, EstadoJuego e) {
        List<EventoJuego> eventos = new ArrayList<>();
        d.detectar(e, eventos::add);
        return eventos;
    }

    @Test
    void balaJugadorDestruyeAlienYOtorgaPuntos() {
        EstadoJuego estado = new EstadoJuego();
        Jugador j = new Jugador("p1", "p1");
        estado.jugadores.agregar(j);
        Squid alien = new Squid("A_1", 100.0, 50.0);
        estado.aliens.agregar(alien);
        Bala bala = new Bala("B_1", 100.0, 50.0, Bala.Direccion.ARRIBA, "p1");
        estado.balas.agregar(bala);

        DetectorColisiones d = new DetectorColisiones();
        List<EventoJuego> eventos = capturar(d, estado);

        assertFalse(alien.estaVivo(), "el alien debe estar destruido");
        assertFalse(bala.estaVivo(), "la bala debe estar destruida");
        assertEquals(Config.PUNTOS_SQUID, j.getPuntaje());
        assertEquals(1, eventos.size());
        assertEquals(EventoJuego.TipoEvento.ALIEN_DESTROYED, eventos.get(0).getTipo());
    }

    @Test
    void balaAlienGolpeaCanonYRestaVida() {
        EstadoJuego estado = new EstadoJuego();
        Jugador j = new Jugador("p1", "p1");
        estado.jugadores.agregar(j);
        Canon canon = new Canon("C_1", "p1", 200.0, Config.CANNON_Y);
        estado.canones.agregar(canon);
        Bala bala = new Bala("B_1", 210.0, Config.CANNON_Y + 2,
            Bala.Direccion.ABAJO, Bala.DUENIO_ALIEN);
        estado.balas.agregar(bala);

        DetectorColisiones d = new DetectorColisiones();
        List<EventoJuego> eventos = capturar(d, estado);

        assertEquals(Config.VIDAS_INICIALES - 1, j.getVidas());
        assertFalse(bala.estaVivo());
        assertTrue(eventos.stream().anyMatch(
            e -> e.getTipo() == EventoJuego.TipoEvento.PLAYER_HIT));
        assertTrue(eventos.stream().anyMatch(
            e -> e.getTipo() == EventoJuego.TipoEvento.PLAYER_LIFE_LOST));
    }

    @Test
    void balaDaniaBunkerEmiteEventoYDiezImpactosLoDestruyen() {
        EstadoJuego estado = new EstadoJuego();
        Bunker bk = new Bunker("BK_1", 200.0, 400.0);
        estado.bunkers.agregar(bk);

        DetectorColisiones d = new DetectorColisiones();

        // primer impacto
        Bala primera = new Bala("B_1", 210.0, 405.0,
            Bala.Direccion.ABAJO, Bala.DUENIO_ALIEN);
        estado.balas.agregar(primera);
        List<EventoJuego> eventos = capturar(d, estado);
        assertTrue(eventos.stream().anyMatch(
            e -> e.getTipo() == EventoJuego.TipoEvento.BUNKER_DAMAGED));
        assertTrue(bk.estaVivo());

        // 9 impactos más para llegar a 0
        for (int i = 0; i < 9; i++) {
            Bala b = new Bala("B_" + (i + 2), 210.0, 405.0,
                Bala.Direccion.ABAJO, Bala.DUENIO_ALIEN);
            estado.balas.agregar(b);
            eventos = capturar(d, estado);
        }

        assertFalse(bk.estaVivo(), "el bunker debe estar destruido tras 10 impactos");
        assertEquals(EventoJuego.TipoEvento.BUNKER_DESTROYED, eventos.get(0).getTipo());
    }

    @Test
    void alienTocandoCanonProvocaGameOver() {
        EstadoJuego estado = new EstadoJuego();
        Jugador j = new Jugador("p1", "p1");
        estado.jugadores.agregar(j);
        Canon canon = new Canon("C_1", "p1", 200.0, Config.CANNON_Y);
        estado.canones.agregar(canon);
        Crab alien = new Crab("A_1", 200.0, Config.CANNON_Y);
        estado.aliens.agregar(alien);

        DetectorColisiones d = new DetectorColisiones();
        List<EventoJuego> eventos = capturar(d, estado);

        assertTrue(estado.juegoTerminado);
        EventoJuego ev = eventos.stream()
            .filter(e -> e.getTipo() == EventoJuego.TipoEvento.GAME_OVER)
            .findFirst().orElse(null);
        assertNotNull(ev, "se debe emitir GAME_OVER");
    }
}
