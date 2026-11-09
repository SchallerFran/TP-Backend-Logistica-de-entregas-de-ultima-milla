package utnfc.isi.back.logistica.despacho.client;

import utnfc.isi.back.logistica.commons.Coordenada;
import utnfc.isi.back.logistica.despacho.config.DespachoProperties;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;

/**
 * Integración con Google Maps Routes API (método computeRouteMatrix, sucesor de Distance Matrix).
 * <p>
 * Una consulta devuelve una matriz origenes × destinos. Sin tráfico (TRAFFIC_UNAWARE) Google admite hasta
 * 625 elementos por request; quien llama (DistanciaService) arma bloques de 10 × 10.
 * <p>
 * Este cliente sólo traduce HTTP/JSON ↔ Java. Las decisiones (caché, reintentos, fallback) se toman
 * en DistanciaService.
 */
@Slf4j
@Component
public class GoogleDistanceMatrixClient {

    public static final int MAX_POR_LADO = 10;

    /** Routes API exige indicar qué campos devolver; sin este header responde 400. */
    static final String FIELD_MASK = "originIndex,destinationIndex,status,condition,distanceMeters,duration";

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

    /** Resultado de un par origen/destino. ok=false si Google no encontró ruta por calle. */
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

        Map<String, Object> request = Map.of(
                "origins", waypoints(origenes),
                "destinations", waypoints(destinos),
                "travelMode", "DRIVE",
                "routingPreference", "TRAFFIC_UNAWARE");

        JsonNode root;
        try {
            root = googleRestClient.post()
                    .uri("/distanceMatrix/v2:computeRouteMatrix")
                    .header("X-Goog-Api-Key", config.apiKey())
                    .header("X-Goog-FieldMask", FIELD_MASK)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientException e) {
            // timeouts, conexión rechazada, 4xx/5xx HTTP (key inválida, API no habilitada, cuota agotada...)
            throw new GoogleNoDisponibleException("Error HTTP consultando Google: " + e.getMessage(), e);
        }

        if (root == null || !root.isArray()) {
            throw new GoogleNoDisponibleException("Respuesta inesperada de Google: se esperaba una lista");
        }

        // La respuesta es una lista plana de elementos, sin orden garantizado.
        Elemento[][] resultado = new Elemento[origenes.size()][destinos.size()];
        for (JsonNode e : root) {
            // En el JSON de Google los campos con valor 0 se omiten: índice ausente = 0
            int i = e.path("originIndex").asInt(0);
            int j = e.path("destinationIndex").asInt(0);
            if (i >= origenes.size() || j >= destinos.size()) {
                throw new GoogleNoDisponibleException("Respuesta inesperada de Google: índice fuera de rango");
            }
            int codigoError = e.path("status").path("code").asInt(0);
            String condicion = e.path("condition").asText("");
            if (codigoError == 0 && "ROUTE_EXISTS".equals(condicion)) {
                resultado[i][j] = new Elemento(true,
                        e.path("distanceMeters").asInt(0),
                        segundos(e.path("duration").asText("0s")),
                        condicion);
            } else {
                // ROUTE_NOT_FOUND (no hay camino por calle) o error puntual de ese par
                String detalle = codigoError != 0 ? e.path("status").path("message").asText("ERROR") : condicion;
                resultado[i][j] = new Elemento(false, 0, 0, detalle);
            }
        }

        for (int i = 0; i < origenes.size(); i++) {
            for (int j = 0; j < destinos.size(); j++) {
                if (resultado[i][j] == null) {
                    throw new GoogleNoDisponibleException("Respuesta incompleta de Google: falta el par " + i + "→" + j);
                }
            }
        }
        return resultado;
    }

    private static List<Map<String, Object>> waypoints(List<Coordenada> puntos) {
        return puntos.stream()
                .map(p -> Map.<String, Object>of("waypoint", Map.of("location", Map.of("latLng",
                        Map.of("latitude", p.latitud(), "longitude", p.longitud())))))
                .toList();
    }

    /** Routes API expresa la duración como "123s" (puede traer decimales). */
    static int segundos(String duracion) {
        return (int) Math.round(Double.parseDouble(duracion.replace("s", "")));
    }
}
