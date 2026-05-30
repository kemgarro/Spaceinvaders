package cr.ac.tec.spaceinvaders.server.entidades;

import cr.ac.tec.spaceinvaders.server.util.Config;

/**
 * Escudo (bunker) de protección terrestre. Estático, absorbe disparos.
 *
 * <p>El estado del bunker se representa con un porcentaje de salud (0–100). Sin
 * embargo, el cliente solo necesita conocer cuatro estados visibles: intacto
 * (100), dañado (70), crítico (40) o destruido (0). El método
 * {@link #getEstadoVisible()} aplica el bucket correspondiente:</p>
 *
 * <ul>
 *   <li>{@code salud == 100}              → 100 (intacto)</li>
 *   <li>{@code 70 <= salud < 100}         → 70  (dañado)</li>
 *   <li>{@code 40 <= salud < 70}          → 40  (crítico)</li>
 *   <li>{@code salud < 40}                → 0   (destruido)</li>
 * </ul>
 *
 * <p>Cada impacto resta {@link Config#BUNKER_DANIO_POR_DISPARO}. Al llegar a
 * cero o menos, el bunker queda destruido ({@code estaVivo() == false}).</p>
 *
 * <p>El método administrativo {@link #fijarSaludAdmin(int)} permite al admin
 * forzar un porcentaje exacto (por ejemplo, el comando {@code "Bunkers 70%"}).</p>
 */
public class Bunker extends EntidadJuego {

    /** Porcentaje actual de salud (0–100). */
    private int salud;

    /**
     * Construye un bunker en estado intacto.
     *
     * @param id identificador único.
     * @param x  posición X inicial.
     * @param y  posición Y inicial.
     */
    public Bunker(String id, double x, double y) {
        super(id, x, y, Config.ANCHO_BUNKER, Config.ALTO_BUNKER);
        this.salud = Config.BUNKER_SALUD_INTACTO;
    }

    /** El bunker es estático: no se mueve por tick. */
    @Override
    public void mover() {
        // intencionalmente vacío
    }

    /**
     * Aplica un impacto al bunker. Resta {@link Config#BUNKER_DANIO_POR_DISPARO}
     * a la salud. Si la salud cae a 0 o menos, el bunker queda destruido.
     */
    public void recibirImpacto() {
        salud -= Config.BUNKER_DANIO_POR_DISPARO;
        if (salud <= 0) {
            salud = Config.BUNKER_SALUD_DESTRUIDO;
            destruir();
        }
    }

    /**
     * @return el estado visible del bunker en buckets: 100, 70, 40 o 0.
     */
    public int getEstadoVisible() {
        if (salud >= Config.BUNKER_SALUD_INTACTO) {
            return Config.BUNKER_SALUD_INTACTO;
        }
        if (salud >= Config.BUNKER_SALUD_DANIADO) {
            return Config.BUNKER_SALUD_DANIADO;
        }
        if (salud >= Config.BUNKER_SALUD_CRITICO) {
            return Config.BUNKER_SALUD_CRITICO;
        }
        return Config.BUNKER_SALUD_DESTRUIDO;
    }

    /** @return porcentaje exacto de salud (0–100). */
    public int getSalud() {
        return salud;
    }

    /**
     * Comando administrativo: fija la salud del bunker a un porcentaje arbitrario.
     * Se aplica clamp a {@code [0, 100]}. Si el valor resultante es 0 el bunker
     * queda destruido; si es mayor a 0 se revive (vivo = true).
     *
     * @param pct porcentaje deseado (será limitado a 0..100).
     */
    public void fijarSaludAdmin(int pct) {
        if (pct < 0) {
            pct = 0;
        }
        if (pct > Config.BUNKER_SALUD_INTACTO) {
            pct = Config.BUNKER_SALUD_INTACTO;
        }
        this.salud = pct;
        if (salud == Config.BUNKER_SALUD_DESTRUIDO) {
            destruir();
        } else {
            this.vivo = true;
        }
    }
}
