package cr.ac.tec.spaceinvaders.server.red;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas de {@link JsonUtil}: serializacion / deserializacion de
 * mensajes JSON line-delimited del protocolo.
 *
 * <p>Cubre los puntos en los que JsonUtil es el contrato entre el motor
 * y el cliente C: cada cambio aqui propaga al binario del cliente que
 * parsea con cJSON.</p>
 */
class JsonUtilTest {

    @Test
    void serializaMensajeConnectConCamposBasicos() {
        Mensaje m = new Mensaje();
        m.setType(Mensaje.TipoMensaje.CONNECT);
        m.setId("p1");
        m.setClientType("PLAYER");

        String json = JsonUtil.toJson(m);

        assertNotNull(json);
        assertTrue(json.contains("\"type\":\"CONNECT\""),
            "el tipo debe estar serializado como CONNECT");
        assertTrue(json.contains("\"id\":\"p1\""), "el id debe persistirse");
        assertTrue(json.contains("\"clientType\":\"PLAYER\""),
            "el clientType debe persistirse");
    }

    @Test
    void deserializaCONNECTConTargetParaEspectador() {
        String json = "{\"type\":\"CONNECT\",\"id\":\"e1\","
                    + "\"clientType\":\"SPECTATOR\",\"target\":\"p1\"}";

        Mensaje m = JsonUtil.fromJson(json);

        assertNotNull(m);
        assertEquals(Mensaje.TipoMensaje.CONNECT, m.getType());
        assertEquals("e1", m.getId());
        assertEquals("SPECTATOR", m.getClientType());
        assertEquals("p1", m.getTarget());
    }

    @Test
    void fromJsonRetornaNullEnJsonInvalido() {
        // JSON sintacticamente invalido: no debe lanzar excepcion, solo retornar null.
        Mensaje m = JsonUtil.fromJson("{ not json }");
        assertNull(m, "fromJson tolera input invalido");
    }

    @Test
    void crearMensajeErrorUsaClaveErrorEnElPayload() {
        // El cliente C lee siempre la clave "error" (ver protocol.c aplicar_error).
        // Este test pinea el contrato.
        String json = JsonUtil.crearMensajeError("test message");

        assertNotNull(json);
        assertTrue(json.contains("\"type\":\"ERROR\""),
            "type debe ser ERROR");
        assertTrue(json.contains("\"payload\""),
            "debe haber payload");
        assertTrue(json.contains("\"error\":\"test message\""),
            "la clave del payload debe ser 'error' (no 'message')");
    }

    @Test
    void crearMensajeEventoIncluyeNameYPayload() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("alienId", "A_5");
        payload.put("puntos", 20);

        String json = JsonUtil.crearMensajeEvento("ALIEN_DESTROYED", payload);

        assertNotNull(json);
        assertTrue(json.contains("\"type\":\"EVENT\""));
        assertTrue(json.contains("\"name\":\"ALIEN_DESTROYED\""));
        assertTrue(json.contains("\"alienId\":\"A_5\""));
        assertTrue(json.contains("\"puntos\":20"));
    }

    @Test
    void crearMensajeEstadoEnvuelveDataEnTipoState() {
        Map<String, Object> estado = new LinkedHashMap<>();
        estado.put("oleada", 1);
        estado.put("juegoTerminado", false);

        String json = JsonUtil.crearMensajeEstado(estado);

        assertNotNull(json);
        assertTrue(json.contains("\"type\":\"STATE\""));
        assertTrue(json.contains("\"data\""));
        assertTrue(json.contains("\"oleada\":1"));
        assertTrue(json.contains("\"juegoTerminado\":false"));
    }

    @Test
    void roundTripConnectMantieneCamposExactos() {
        Mensaje original = new Mensaje();
        original.setType(Mensaje.TipoMensaje.INPUT);
        original.setId("p2");
        original.setAction("FIRE");

        String json = JsonUtil.toJson(original);
        Mensaje roundTrip = JsonUtil.fromJson(json);

        assertNotNull(roundTrip);
        assertEquals(original.getType(), roundTrip.getType());
        assertEquals(original.getId(), roundTrip.getId());
        assertEquals(original.getAction(), roundTrip.getAction());
    }

    @Test
    void disconnectSerializaSoloConIdYTipo() {
        Mensaje m = new Mensaje();
        m.setType(Mensaje.TipoMensaje.DISCONNECT);
        m.setId("p1");

        String json = JsonUtil.toJson(m);
        Mensaje parsed = JsonUtil.fromJson(json);

        assertNotNull(parsed);
        assertEquals(Mensaje.TipoMensaje.DISCONNECT, parsed.getType());
        assertEquals("p1", parsed.getId());
    }
}
