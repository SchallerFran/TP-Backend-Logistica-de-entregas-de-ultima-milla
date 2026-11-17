package utnfc.isi.back.logistica.despacho.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Slf4j
@Configuration
public class RestClientConfig {

    public static final String REGISTRACION_KEYCLOAK = "logistica-service";

    @Bean
    OAuth2AuthorizedClientManager authorizedClientManager(ClientRegistrationRepository registraciones) {
        var clientService = new InMemoryOAuth2AuthorizedClientService(registraciones);
        var manager = new AuthorizedClientServiceOAuth2AuthorizedClientManager(registraciones, clientService);
        manager.setAuthorizedClientProvider(OAuth2AuthorizedClientProviderBuilder.builder()
                .clientCredentials()
                .build());
        return manager;
    }

    @Bean
    @Qualifier("pedidosRestClient")
    RestClient pedidosRestClient(DespachoProperties props, OAuth2AuthorizedClientManager manager) {
        ClientHttpRequestInterceptor bearer = (request, body, execution) -> {
            OAuth2AuthorizeRequest authorizeRequest = OAuth2AuthorizeRequest
                    .withClientRegistrationId(REGISTRACION_KEYCLOAK)
                    .principal(REGISTRACION_KEYCLOAK)
                    .build();
            OAuth2AuthorizedClient client = manager.authorize(authorizeRequest);
            if (client == null) {
                throw new IllegalStateException("No se pudo obtener token de servicio desde Keycloak");
            }
            request.getHeaders().setBearerAuth(client.getAccessToken().getTokenValue());
            return execution.execute(request, body);
        };

        return RestClient.builder()
                .baseUrl(props.pedidos().baseUrl())
                .requestFactory(requestFactory(props.pedidos().connectTimeout(), props.pedidos().readTimeout()))
                .requestInterceptor(bearer)
                .requestInterceptor(logging("pedidos-service"))
                .build();
    }

    @Bean
    @Qualifier("osrmRestClient")
    RestClient osrmRestClient(DespachoProperties props) {
        var osrm = props.distancias().osrm();
        return RestClient.builder()
                .baseUrl(osrm.baseUrl())
                .requestFactory(requestFactory(osrm.connectTimeout(), osrm.readTimeout()))
                .build();
    }

    private static SimpleClientHttpRequestFactory requestFactory(Duration connect, Duration read) {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connect);
        factory.setReadTimeout(read);
        return factory;
    }

    private static ClientHttpRequestInterceptor logging(String destino) {
        return (request, body, execution) -> {
            long t0 = System.currentTimeMillis();
            var response = execution.execute(request, body);
            log.debug("→ {} {} {} | ← {} | {} ms", destino, request.getMethod(), request.getURI(),
                    response.getStatusCode().value(), System.currentTimeMillis() - t0);
            return response;
        };
    }
}
