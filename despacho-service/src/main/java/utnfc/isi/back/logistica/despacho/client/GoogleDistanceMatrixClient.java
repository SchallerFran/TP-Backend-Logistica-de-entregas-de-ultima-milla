package utnfc.isi.back.logistica.despacho.client;

import utnfc.isi.back.logistica.despacho.geo.Coordenada;
import utnfc.isi.back.logistica.despacho.config.DespachoProperties;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Integración con Google Maps Distance Matrix API.
 * <p>
 * Una consulta devuelve una matriz origenes × destinos. Google limita a 25 orígenes, 25 destinos y
 * 100 elementos por request; quien llama (DistanciaService) arma bloques de 10 × 10.
 * <p>
 * Este cliente sólo traduce HTTP/JSON ↔ Java. Las decisiones (caché, reintentos, fallback) se toman
 * en DistanciaService.
 */
@Slf4j
@Component
public class GoogleDistanceMatrixClient {

    public static final int MAX_POR_LADO = 10;

    private final RestClient googleRestClient;
    private final DespachoProperties.Google config;

    public GoogleDistanceMatrixClient(@Qualifier("googleRestClient") RestClient googleRestClient,
                                      DespachoProperties props) {
        this.googleRestClient = googleRestClient;
        this.config = props.distancias().google();
    }

    public boolean habilitado() {
        return config.habilitada();
    }

    /** Resultado de un par origen/destino. ok=false si Google no encontró ruta (NOT_FOUND, ZERO_RESULTS). */
    public record Elemento(boolean ok, int metros, int segundos, String status) {
    }

    /**
     * @return matriz [i][j] con el resultado de origenes[i] → destinos[j]
     * @throws GoogleNoDisponibleException si no hubo respuesta utilizable
     */
    public Elemento[][] consultar(List<Coordenada> origenes, List<Coordenada> destinos) {
        if (!habilitado()) {
            throw new GoogleNoDisponibleException("Google Maps deshabilitado (sin API key)");
        }
        if (origenes.size() > MAX_POR_LADO || destinos.size() > MAX_POR_LADO) {
            throw new IllegalArgumentException("Máximo " + MAX_POR_LADO + " orígenes/destinos por consulta");
        }

        JsonNode root;
        try {
            root = googleRestClient.get()
                    .uri(uri -> uri.path("/distancematrix/json")
                            .queryParam("origins", unir(origenes))
                            .queryParam("destinations", unir(destinos))
                            .queryParam("mode", "driving")
                            .queryParam("units", "metric")
                            .queryParam("key", config.apiKey())
                            .build())
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientException e) {
            // timeouts, conexión rechazada, 4xx/5xx HTTP
            throw new GoogleNoDisponibleException("Error HTTP consultando Google: " + e.getMessage(), e);
        }

        if (root == null) {
            throw new GoogleNoDisponibleException("Respuesta vacía de Google");
        }
        String status = root.path("status").asText("");
        if (!"OK".equals(status)) {
            // OVER_QUERY_LIMIT, OVER_DAILY_LIMIT, REQUEST_DENIED, INVALID_REQUEST, MAX_ELEMENTS_EXCEEDED, UNKNOWN_ERROR...
            String detalle = root.path("error_message").asText("");
            throw new GoogleNoDisponibleException("Google respondió status=" + status + " " + detalle);
        }

        JsonNode rows = root.path("rows");
        if (!rows.isArray() || rows.size() != origenes.size()) {
            throw new GoogleNoDisponibleException("Respuesta inesperada de Google: cantidad de filas");
        }

        Elemento[][] resultado = new Elemento[origenes.size()][destinos.size()];
        for (int i = 0; i < origenes.size(); i++) {
            JsonNode elements = rows.get(i).path("elements");
            if (!elements.isArray() || elements.size() != destinos.size()) {
                throw new GoogleNoDisponibleException("Respuesta inesperada de Google: cantidad de columnas");
            }
            for (int j = 0; j < destinos.size(); j++) {
                JsonNode e = elements.get(j);
                String st = e.path("status").asText("");
                if ("OK".equals(st) && e.path("distance").has("value") && e.path("duration").has("value")) {
                    resultado[i][j] = new Elemento(true,
                            e.path("distance").path("value").asInt(),
                            e.path("duration").path("value").asInt(),
                            st);
                } else {
                    // NOT_FOUND (coordenada no geocodificable) o ZERO_RESULTS (no hay ruta por calle)
                    resultado[i][j] = new Elemento(false, 0, 0, st);
                }
            }
        }
        return resultado;
    }

    private static String unir(List<Coordenada> puntos) {
        return puntos.stream().map(Coordenada::comoLatLng).collect(Collectors.joining("|"));
    }
}
