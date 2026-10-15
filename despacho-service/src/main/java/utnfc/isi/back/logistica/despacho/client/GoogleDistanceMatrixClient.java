package utnfc.isi.back.logistica.despacho.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Cliente de Google Distance Matrix. Esqueleto: sin llamadas reales todavia.
 * La API key llega por configuracion (GOOGLE_MAPS_API_KEY), nunca hardcodeada.
 */
@Component
public class GoogleDistanceMatrixClient {

    private final RestClient googleMapsRestClient;
    private final String apiKey;

    public GoogleDistanceMatrixClient(RestClient googleMapsRestClient,
                                      @Value("${google.maps.apikey}") String apiKey) {
        this.googleMapsRestClient = googleMapsRestClient;
        this.apiKey = apiKey;
    }

    protected RestClient cliente() {
        return googleMapsRestClient;
    }

    protected String apiKey() {
        return apiKey;
    }
}
