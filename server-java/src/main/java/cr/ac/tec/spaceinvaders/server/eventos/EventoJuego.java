package cr.ac.tec.spaceinvaders.server.eventos;

/**
 * Representa un evento del juego.
 * Se usa para notificar cambios importantes como destrucción de aliens,
 * impactos, fin de oleada, etc.
 *
 * ADAPTADO DEL PROYECTO DonCEyKongJr.
 * Tipos de evento renombrados para Space Invaders.
 */
public class EventoJuego {
    public enum TipoEvento {
        ALIEN_DESTROYED,    // Un alien fue destruido (incluye killer_id, points)
        UFO_DESTROYED,      // El OVNI fue destruido (incluye killer_id, points aleatorios)
        PLAYER_HIT,         // Jugador golpeado por bala alien
        PLAYER_LIFE_LOST,   // Jugador perdió una vida
        PLAYER_LIFE_GAINED, // Jugador ganó una vida (al limpiar oleada)
        PLAYER_ELIMINATED,  // Jugador eliminado (vidas = 0)
        WAVE_CLEARED,       // Oleada limpiada (todos los aliens destruidos)
        WAVE_STARTED,       // Nueva oleada comenzó
        BUNKER_DAMAGED,     // Bunker recibió daño
        BUNKER_DESTROYED,   // Bunker totalmente destruido
        GAME_OVER,          // Fin del juego
        SPEED_CHANGED       // Cambio de velocidad de aliens
    }

    private TipoEvento tipo;
    private Object payload; // Datos adicionales del evento

    /**
     * Crea un nuevo evento.
     */
    public EventoJuego(TipoEvento tipo, Object payload) {
        this.tipo = tipo;
        this.payload = payload;
    }

    /**
     * Crea un evento sin payload.
     */
    public EventoJuego(TipoEvento tipo) {
        this(tipo, null);
    }

    public TipoEvento getTipo() {
        return tipo;
    }

    public Object getPayload() {
        return payload;
    }

    @Override
    public String toString() {
        return "EventoJuego{" +
                "tipo=" + tipo +
                ", payload=" + payload +
                '}';
    }
}
