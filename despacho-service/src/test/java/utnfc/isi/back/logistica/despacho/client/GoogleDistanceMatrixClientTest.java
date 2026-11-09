package utnfc.isi.back.logistica.despacho.client;

import utnfc.isi.back.logistica.commons.Coordenada;
import utnfc.isi.back.logistica.despacho.client.GoogleDistanceMatrixClient.Elemento;
import utnfc.isi.back.logistica.despacho.config.DespachoProperties;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/** Traducción de la respuesta de Routes API (computeRouteMatrix) a la matriz. Sin red: el servidor es un mock. */
class GoogleDistanceMatrixClientTest {

    private static final String URL = "https://routes.googleapis.com/distanceMatrix/v2:computeRouteMatrix";
    private static final List<Coordenada> PUNTOS =
            List.of(Coordenada.of(-31.4135, -64.1810), Coordenada.of(-31.4400, -64.2200));

    private final RestClient.Builder builder = RestClient.builder().baseUrl("https://routes.googleapis.com");
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    private final GoogleDistanceMatrixClient client = new GoogleDistanceMatrixClient(builder.build(),
            new DespachoProperties(null, null, null, null, new DespachoProperties.Distancias(
                    new DespachoProperties.Google("https://routes.googleapis.com", "clave-test", null, null),
                    30, 1.3, 30)));

    @Test
    void armaLaMatrizAunqueGoogleOmitaLosIndicesCeroYDesordeneLosElementos() {
        server.expect(requestTo(URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-Goog-Api-Key", "clave-test"))
                .andExpect(header("X-Goog-FieldMask", GoogleDistanceMatrixClient.FIELD_MASK))
                .andExpect(jsonPath("$.origins[1].waypoint.location.latLng.latitude").value(-31.44))
                .andRespond(withSuccess("""
                        [
                          {"originIndex": 1, "destinationIndex": 1, "status": {}, "condition": "ROUTE_EXISTS", "duration": "0s"},
                          {"destinationIndex": 1, "status": {}, "distanceMeters": 5230, "duration": "612.4s", "condition": "ROUTE_EXISTS"},
                          {"status": {}, "duration": "0s", "condition": "ROUTE_EXISTS"},
                          {"originIndex": 1, "status": {}, "condition": "ROUTE_NOT_FOUND"}
                        ]""", MediaType.APPLICATION_JSON));

        Elemento[][] m = client.consultar(PUNTOS, PUNTOS);

        assertEquals(new Elemento(true, 0, 0, "ROUTE_EXISTS"), m[0][0]);
        assertEquals(new Elemento(true, 5230, 612, "ROUTE_EXISTS"), m[0][1]);
        assertFalse(m[1][0].ok());
        assertEquals(new Elemento(true, 0, 0, "ROUTE_EXISTS"), m[1][1]);
        server.verify();
    }

    @Test
    void siFaltaUnParLaRespuestaNoSeUsa() {
        server.expect(requestTo(URL)).andRespond(withSuccess(
                "[{\"status\": {}, \"condition\": \"ROUTE_EXISTS\", \"duration\": \"0s\"}]", MediaType.APPLICATION_JSON));

        assertThrows(GoogleNoDisponibleException.class, () -> client.consultar(PUNTOS, PUNTOS));
    }

    @Test
    void unErrorHttpDeGoogleSeTraduceANoDisponible() {
        server.expect(requestTo(URL)).andRespond(withStatus(org.springframework.http.HttpStatus.FORBIDDEN)
                .body("{\"error\": {\"code\": 403, \"status\": \"PERMISSION_DENIED\"}}")
                .contentType(MediaType.APPLICATION_JSON));

        assertThrows(GoogleNoDisponibleException.class, () -> client.consultar(PUNTOS, PUNTOS));
    }
}
