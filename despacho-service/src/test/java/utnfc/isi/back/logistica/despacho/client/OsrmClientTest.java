package utnfc.isi.back.logistica.despacho.client;

import utnfc.isi.back.logistica.commons.Coordenada;
import utnfc.isi.back.logistica.despacho.client.OsrmClient.Elemento;
import utnfc.isi.back.logistica.despacho.config.DespachoProperties;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/** Traducción de la respuesta de OSRM (servicio table) a la matriz. Sin red: el servidor es un mock. */
class OsrmClientTest {

    private static final String BASE = "https://osrm.test";
    private static final List<Coordenada> ORIGENES = List.of(Coordenada.of(-31.4135, -64.181));
    private static final List<Coordenada> DESTINOS =
            List.of(Coordenada.of(-31.44, -64.22), Coordenada.of(-34.6037, -58.3816));
    // longitud,latitud; orígenes primero y después destinos
    private static final String URL = BASE + "/table/v1/driving/-64.181,-31.4135;-64.22,-31.44;-58.3816,-34.6037"
            + "?sources=0&destinations=1;2&annotations=distance,duration";

    private final RestClient.Builder builder = RestClient.builder().baseUrl(BASE);
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    private final OsrmClient client = new OsrmClient(builder.build(),
            new DespachoProperties(null, null, null, null, new DespachoProperties.Distancias(
                    new DespachoProperties.Osrm(BASE, null, null), 30, 1.3, 30)));

    @Test
    void armaLaMatrizConMetrosYSegundosRedondeadosYMarcaLosParesSinRuta() {
        server.expect(requestTo(URL))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {"code": "Ok",
                         "distances": [[6241.4, null]],
                         "durations": [[612.6, null]]}""", MediaType.APPLICATION_JSON));

        Elemento[][] m = client.consultar(ORIGENES, DESTINOS);

        assertEquals(new Elemento(true, 6241, 613, "Ok"), m[0][0]);
        assertFalse(m[0][1].ok());
        server.verify();
    }

    @Test
    void unCodeDistintoDeOkSeTraduceANoDisponible() {
        server.expect(requestTo(URL)).andRespond(withSuccess(
                "{\"code\": \"InvalidQuery\", \"message\": \"Query string malformed\"}", MediaType.APPLICATION_JSON));

        assertThrows(RuteoNoDisponibleException.class, () -> client.consultar(ORIGENES, DESTINOS));
    }

    @Test
    void elLimiteDelServidorPublicoSeTraduceANoDisponible() {
        server.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        assertThrows(RuteoNoDisponibleException.class, () -> client.consultar(ORIGENES, DESTINOS));
    }
}
