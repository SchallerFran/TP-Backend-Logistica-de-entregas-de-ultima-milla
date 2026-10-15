package utnfc.isi.back.logistica.despacho.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Bean
    public RestClient pedidosRestClient(RestClient.Builder builder,
                                        @Value("${servicios.pedidos.url}") String pedidosUrl) {
        return builder.baseUrl(pedidosUrl).build();
    }

    @Bean
    public RestClient googleMapsRestClient(RestClient.Builder builder,
                                           @Value("${google.maps.distance-matrix-url}") String distanceMatrixUrl) {
        return builder.baseUrl(distanceMatrixUrl).build();
    }
}
