package cr.ac.tec.spaceinvaders.server.nucleo;

import cr.ac.tec.spaceinvaders.server.entidades.Alien;
import cr.ac.tec.spaceinvaders.server.entidades.Bala;
import cr.ac.tec.spaceinvaders.server.entidades.Bunker;
import cr.ac.tec.spaceinvaders.server.entidades.Canon;
import cr.ac.tec.spaceinvaders.server.entidades.Jugador;
import cr.ac.tec.spaceinvaders.server.entidades.Ovni;
import cr.ac.tec.spaceinvaders.server.eventos.EventoJuego;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Detector de colisiones del motor.
 *
 * <p>Recorre las entidades vivas del {@link EstadoJuego} aplicando reglas AABB
 * (definidas en {@link cr.ac.tec.spaceinvaders.server.entidades.EntidadJuego#colisionaCon})
 * y registra los efectos: aliens destruidos, OVNI derribado, jugadores
 * impactados, bunkers dañados y la condición especial de "aliens alcanzando
 * el cañón" (Game Over).</p>
 *
 * <p>Para evitar acoplar este detector con {@link MotorJuego}, los eventos
 * se publican a través de un {@link SinkEventos} (callback). El motor pasa un
 * sink que recolecta los eventos para luego notificar a los observadores
 * fuera del ciclo de detección.</p>
 *
 * <p><strong>Patrón de iteración seguro:</strong> las balas/aliens a eliminar
 * se recolectan en listas temporales y se eliminan al final del recorrido,
 * porque {@link cr.ac.tec.spaceinvaders.server.estructuras.ListaEnlazada} no
 * tolera modificaciones durante {@code for-each}.</p>
 */
public class DetectorColisiones {

    /** Callback para emitir eventos durante la detección. */
    public interface SinkEventos {
        /** Emite un evento producido por la detección. */
        void emitir(EventoJuego e);
    }

    /**
     * Procesa todas las colisiones de un tick. Modifica el estado.
     *
     * <p>Reglas aplicadas (en este orden):</p>
     * <ol>
     *   <li>Aliens vs cañón: si algún alien toca un cañón vivo, se marca fin
     *       de juego y se emite {@link EventoJuego.TipoEvento#GAME_OVER}.</li>
     *   <li>Bala de jugador vs alien: el alien muere y el jugador suma puntos.</li>
     *   <li>Bala de jugador vs OVNI: el OVNI muere y el jugador suma puntos
     *       aleatorios.</li>
     *   <li>Bala de alien vs cañón: el jugador pierde una vida.</li>
     *   <li>Cualquier bala vs bunker vivo: el bunker recibe impacto.</li>
     * </ol>
     *
     * @param estado estado del juego a procesar.
     * @param sink   destino de los eventos emitidos.
     */
    public void detectar(EstadoJuego estado, SinkEventos sink) {
        // Caso especial: si los aliens llegaron al cañón, fin de juego.
        if (detectarAliensTocanCanon(estado, sink)) {
            return;
        }

        List<Bala> balasMuertas = new ArrayList<>();
        List<Alien> aliensMuertos = new ArrayList<>();

        for (Bala bala : estado.balas) {
            if (!bala.estaVivo()) continue;

            if (bala.esDelJugador()) {
                if (procesarBalaJugadorContraAliens(bala, estado, sink, aliensMuertos)) {
                    balasMuertas.add(bala);
                    continue;
                }
                if (procesarBalaJugadorContraOvni(bala, estado, sink)) {
                    balasMuertas.add(bala);
                    continue;
                }
            } else {
                if (procesarBalaAlienContraCanones(bala, estado, sink)) {
                    balasMuertas.add(bala);
                    continue;
                }
            }

            if (procesarBalaContraBunkers(bala, estado, sink)) {
                balasMuertas.add(bala);
            }
        }

        for (Bala b : balasMuertas) {
            estado.balas.eliminar(b);
        }
        for (Alien a : aliensMuertos) {
            estado.aliens.eliminar(a);
        }
    }

    // ============================================================
    // helpers internos
    // ============================================================

    private boolean detectarAliensTocanCanon(EstadoJuego estado, SinkEventos sink) {
        for (Alien a : estado.aliens) {
            if (!a.estaVivo()) continue;
            for (Canon c : estado.canones) {
                if (!c.estaVivo()) continue;
                if (a.colisionaCon(c)) {
                    estado.juegoTerminado = true;
                    Map<String, Object> payload = new LinkedHashMap<>();
                    payload.put("razon", "ALIENS_REACHED_CANNON");
                    sink.emitir(new EventoJuego(EventoJuego.TipoEvento.GAME_OVER, payload));
                    return true;
                }
            }
        }
        return false;
    }

    private boolean procesarBalaJugadorContraAliens(Bala bala,
                                                    EstadoJuego estado,
                                                    SinkEventos sink,
                                                    List<Alien> aliensMuertos) {
        for (Alien a : estado.aliens) {
            if (!a.estaVivo()) continue;
            if (bala.colisionaCon(a)) {
                int puntos = a.getPuntos();
                a.destruir();
                aliensMuertos.add(a);
                bala.destruir();

                Jugador asesino = buscarJugador(estado, bala.getDuenioId());
                if (asesino != null) {
                    asesino.agregarPuntos(puntos);
                }

                Map<String, Object> payload = new LinkedHashMap<>();
                payload.put("alienId", a.getId());
                payload.put("killerId", bala.getDuenioId());
                payload.put("puntos", puntos);
                sink.emitir(new EventoJuego(EventoJuego.TipoEvento.ALIEN_DESTROYED, payload));
                return true;
            }
        }
        return false;
    }

    private boolean procesarBalaJugadorContraOvni(Bala bala,
                                                  EstadoJuego estado,
                                                  SinkEventos sink) {
        Ovni ovni = estado.ovni;
        if (ovni == null || !ovni.estaVivo()) return false;
        if (!bala.colisionaCon(ovni)) return false;

        int puntos = ovni.otorgarPuntosAleatorios();
        ovni.destruir();
        bala.destruir();

        Jugador asesino = buscarJugador(estado, bala.getDuenioId());
        if (asesino != null) {
            asesino.agregarPuntos(puntos);
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("ufoId", ovni.getId());
        payload.put("killerId", bala.getDuenioId());
        payload.put("puntos", puntos);
        sink.emitir(new EventoJuego(EventoJuego.TipoEvento.UFO_DESTROYED, payload));
        return true;
    }

    private boolean procesarBalaAlienContraCanones(Bala bala,
                                                   EstadoJuego estado,
                                                   SinkEventos sink) {
        for (Canon c : estado.canones) {
            if (!c.estaVivo()) continue;
            if (bala.colisionaCon(c)) {
                Jugador victima = buscarJugador(estado, c.getJugadorId());
                bala.destruir();
                if (victima == null) {
                    return true;
                }

                victima.perderVida();
                int vidasRestantes = victima.getVidas();

                Map<String, Object> hitPayload = new LinkedHashMap<>();
                hitPayload.put("playerId", victima.getId());
                hitPayload.put("vidasRestantes", vidasRestantes);
                sink.emitir(new EventoJuego(EventoJuego.TipoEvento.PLAYER_HIT, hitPayload));

                Map<String, Object> lostPayload = new LinkedHashMap<>();
                lostPayload.put("playerId", victima.getId());
                lostPayload.put("vidasRestantes", vidasRestantes);
                sink.emitir(new EventoJuego(EventoJuego.TipoEvento.PLAYER_LIFE_LOST, lostPayload));

                if (vidasRestantes <= 0) {
                    Map<String, Object> elimPayload = new LinkedHashMap<>();
                    elimPayload.put("playerId", victima.getId());
                    sink.emitir(new EventoJuego(EventoJuego.TipoEvento.PLAYER_ELIMINATED, elimPayload));
                }
                return true;
            }
        }
        return false;
    }

    private boolean procesarBalaContraBunkers(Bala bala,
                                              EstadoJuego estado,
                                              SinkEventos sink) {
        for (Bunker bk : estado.bunkers) {
            if (!bk.estaVivo()) continue;
            if (bala.colisionaCon(bk)) {
                bk.recibirImpacto();
                bala.destruir();

                if (bk.estaVivo() && bk.getSalud() > 0) {
                    Map<String, Object> payload = new LinkedHashMap<>();
                    payload.put("bunkerId", bk.getId());
                    payload.put("salud", bk.getSalud());
                    payload.put("estadoVisible", bk.getEstadoVisible());
                    sink.emitir(new EventoJuego(EventoJuego.TipoEvento.BUNKER_DAMAGED, payload));
                } else {
                    Map<String, Object> payload = new LinkedHashMap<>();
                    payload.put("bunkerId", bk.getId());
                    sink.emitir(new EventoJuego(EventoJuego.TipoEvento.BUNKER_DESTROYED, payload));
                }
                return true;
            }
        }
        return false;
    }

    private Jugador buscarJugador(EstadoJuego estado, String id) {
        if (id == null) return null;
        for (Jugador j : estado.jugadores) {
            if (id.equals(j.getId())) return j;
        }
        return null;
    }
}
