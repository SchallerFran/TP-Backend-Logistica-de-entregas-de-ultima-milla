package utnfc.isi.back.logistica.pedidos.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Bean
    public RestClient despachoRestClient(RestClient.Builder builder,
                                         @Value("${servicios.despacho.url}") String despachoUrl) {
        return builder.baseUrl(despachoUrl).build();
    }
}
