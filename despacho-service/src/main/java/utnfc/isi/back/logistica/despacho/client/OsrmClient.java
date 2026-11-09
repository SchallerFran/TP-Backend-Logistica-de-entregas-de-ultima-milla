package utnfc.isi.back.logistica.despacho.client;

import utnfc.isi.back.logistica.commons.Coordenada;
import utnfc.isi.back.logistica.despacho.config.DespachoProperties;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Integración con OSRM (Open Source Routing Machine), servicio de ruteo sobre datos de OpenStreetMap.
 * <p>
 * Usa el servicio {@code table}: una sola consulta devuelve la matriz origenes × destinos de
 * distancias (m) y tiempos (s) por calle. OSRM admite hasta 100 coordenadas por consulta (sumando
 * orígenes y destinos); quien llama (DistanciaService) arma bloques de 50 × 50.
 * <p>
 * El servidor es configurable: el público ({@code router.project-osrm.org}) o uno propio levantado
 * con el mapa de Argentina. Ninguno pide API key.
 * <p>
 * Este cliente sólo traduce HTTP/JSON ↔ Java. Las decisiones (caché, reintentos, fallback) se toman
 * en DistanciaService.
 */
@Slf4j
@Component
public class OsrmClient {

    public static final int MAX_POR_LADO = 50;

    private final RestClient osrmRestClient;
    private final DespachoProperties.Osrm config;

    public OsrmClient(@Qualifier("osrmRestClient") RestClient osrmRestClient, DespachoProperties props) {
        this.osrmRestClient = osrmRestClient;
        this.config = props.distancias().osrm();
    }

    public boolean habilitado() {
        return config.habilitado();
    }

    /** Resultado de un par origen/destino. ok=false si OSRM no encontró ruta por calle. */
    public record Elemento(boolean ok, int metros, int segundos, String status) {
    }

    /**
     * @return matriz [i][j] con el resultado de origenes[i] → destinos[j]
     * @throws RuteoNoDisponibleException si no hubo respuesta utilizable
     */
    public Elemento[][] consultar(List<Coordenada> origenes, List<Coordenada> destinos) {
        if (!habilitado()) {
            throw new RuteoNoDisponibleException("OSRM deshabilitado (sin URL configurada)");
        }
        if (origenes.size() > MAX_POR_LADO || destinos.size() > MAX_POR_LADO) {
            throw new IllegalArgumentException("Máximo " + MAX_POR_LADO + " orígenes/destinos por consulta");
        }

        // Se mandan orígenes y destinos juntos; sources/destinations indican qué posiciones son cada cosa.
        String coordenadas = coordenadas(origenes) + ";" + coordenadas(destinos);
        String sources = indices(0, origenes.size());
        String destinations = indices(origenes.size(), origenes.size() + destinos.size());

        JsonNode root;
        try {
            root = osrmRestClient.get()
                    .uri(uri -> uri.path("/table/v1/driving/" + coordenadas)
                            .queryParam("sources", sources)
                            .queryParam("destinations", destinations)
                            .queryParam("annotations", "distance,duration")
                            .build())
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientException e) {
            // timeouts, conexión rechazada, 4xx/5xx HTTP (429 si se supera el límite del servidor público)
            throw new RuteoNoDisponibleException("Error HTTP consultando OSRM: " + e.getMessage(), e);
        }

        if (root == null) {
            throw new RuteoNoDisponibleException("Respuesta vacía de OSRM");
        }
        String code = root.path("code").asText("");
        if (!"Ok".equals(code)) {
            throw new RuteoNoDisponibleException("OSRM respondió code=" + code + " " + root.path("message").asText(""));
        }

        JsonNode distancias = root.path("distances");
        JsonNode duraciones = root.path("durations");
        if (!distancias.isArray() || distancias.size() != origenes.size()
                || !duraciones.isArray() || duraciones.size() != origenes.size()) {
            throw new RuteoNoDisponibleException("Respuesta inesperada de OSRM: cantidad de filas");
        }

        Elemento[][] resultado = new Elemento[origenes.size()][destinos.size()];
        for (int i = 0; i < origenes.size(); i++) {
            JsonNode filaD = distancias.get(i);
            JsonNode filaT = duraciones.get(i);
            if (filaD.size() != destinos.size() || filaT.size() != destinos.size()) {
                throw new RuteoNoDisponibleException("Respuesta inesperada de OSRM: cantidad de columnas");
            }
            for (int j = 0; j < destinos.size(); j++) {
                JsonNode d = filaD.get(j);
                JsonNode t = filaT.get(j);
                if (d.isNumber() && t.isNumber()) {
                    resultado[i][j] = new Elemento(true, (int) Math.round(d.asDouble()), (int) Math.round(t.asDouble()), "Ok");
                } else {
                    // null: no hay camino por calle entre esos dos puntos
                    resultado[i][j] = new Elemento(false, 0, 0, "SIN_RUTA");
                }
            }
        }
        return resultado;
    }

    /** OSRM recibe "longitud,latitud" (primero la longitud) separados por ";". */
    private static String coordenadas(List<Coordenada> puntos) {
        // Double.toString no depende del Locale: siempre usa punto decimal
        return puntos.stream().map(p -> p.longitud() + "," + p.latitud()).collect(Collectors.joining(";"));
    }

    private static String indices(int desde, int hasta) {
        return IntStream.range(desde, hasta).mapToObj(String::valueOf).collect(Collectors.joining(";"));
    }
}
