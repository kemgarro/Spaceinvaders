package cr.ac.tec.spaceinvaders.server.nucleo;

import cr.ac.tec.spaceinvaders.server.entidades.Alien;
import cr.ac.tec.spaceinvaders.server.entidades.Jugador;
import cr.ac.tec.spaceinvaders.server.entidades.TipoAlien;
import cr.ac.tec.spaceinvaders.server.eventos.EventoJuego;
import cr.ac.tec.spaceinvaders.server.util.Config;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GestorOleadasTest {

    @Test
    void prepararInicioCreaCuatroBunkersYCincuentaYCincoAliens() {
        GestorOleadas gestor = new GestorOleadas();
        EstadoJuego estado = new EstadoJuego();

        gestor.prepararInicio(estado);

        assertEquals(Config.NUM_BUNKERS, estado.bunkers.tamano());
        assertEquals(55, estado.aliens.tamano(), "5 filas x 11 columnas = 55 aliens");
        assertEquals(1, estado.oleada);

        // Conteo por tipo: 11 squid, 22 crab, 22 octopus.
        Map<TipoAlien, Integer> conteo = new HashMap<>();
        for (Alien a : estado.aliens) {
            conteo.merge(a.getTipo(), 1, Integer::sum);
        }
        assertEquals(11, conteo.getOrDefault(TipoAlien.SQUID, 0));
        assertEquals(22, conteo.getOrDefault(TipoAlien.CRAB, 0));
        assertEquals(22, conteo.getOrDefault(TipoAlien.OCTOPUS, 0));
    }

    @Test
    void siguienteOleadaAgregaVidaReduceIntervaloYRegeneraAliens() {
        GestorOleadas gestor = new GestorOleadas();
        EstadoJuego estado = new EstadoJuego();
        gestor.prepararInicio(estado);

        Jugador j1 = new Jugador("p1", "p1");
        Jugador j2 = new Jugador("p2", "p2");
        estado.jugadores.agregar(j1);
        estado.jugadores.agregar(j2);
        int vidasIniciales = j1.getVidas();
        long intervaloPrev = estado.intervaloAliensMs;
        estado.aliens.vaciar(); // simulamos que la oleada quedó limpia

        List<EventoJuego> eventos = new ArrayList<>();
        gestor.siguienteOleada(estado, eventos::add);

        assertEquals(vidasIniciales + 1, j1.getVidas());
        assertEquals(vidasIniciales + 1, j2.getVidas());
        assertEquals(55, estado.aliens.tamano(), "se regenera la formación 5x11");
        assertTrue(estado.intervaloAliensMs <= intervaloPrev);
        assertTrue(eventos.stream().anyMatch(
            e -> e.getTipo() == EventoJuego.TipoEvento.PLAYER_LIFE_GAINED));
        assertTrue(eventos.stream().anyMatch(
            e -> e.getTipo() == EventoJuego.TipoEvento.WAVE_STARTED));
    }
}
