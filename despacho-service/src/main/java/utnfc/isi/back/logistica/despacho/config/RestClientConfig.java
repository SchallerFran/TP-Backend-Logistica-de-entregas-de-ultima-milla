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

/**
 * Clientes HTTP salientes (RestClient, como en el Apunte 18), cada uno con sus timeouts:
 * sin timeout, un servicio colgado deja colgado también a quien lo llama.
 */
@Slf4j
@Configuration
public class RestClientConfig {

    /** Id de la registración OAuth2 en application.yml (spring.security.oauth2.client.registration.*). */
    public static final String REGISTRACION_KEYCLOAK = "logistica-service";

    /**
     * Gestor de tokens para client credentials. Se usa la variante "AuthorizedClientService" (y no la
     * Default) porque las llamadas también salen desde el scheduler, donde no hay request HTTP en curso.
     * Cachea el token y lo renueva solo cuando vence.
     */
    @Bean
    OAuth2AuthorizedClientManager authorizedClientManager(ClientRegistrationRepository registraciones) {
        var clientService = new InMemoryOAuth2AuthorizedClientService(registraciones);
        var manager = new AuthorizedClientServiceOAuth2AuthorizedClientManager(registraciones, clientService);
        manager.setAuthorizedClientProvider(OAuth2AuthorizedClientProviderBuilder.builder()
                .clientCredentials()
                .build());
        return manager;
    }

    /**
     * Cliente hacia pedidos-service. Cada request lleva "Authorization: Bearer <token de despacho>",
     * token que tiene el rol SERVICE y por lo tanto puede entrar a /internal/** de pedidos.
     * El token propaga la identidad del servicio, no la del usuario: /internal no debe depender de roles humanos.
     */
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

    /** Cliente hacia Google Maps. La API key va como query param (la agrega GoogleDistanceMatrixClient). */
    @Bean
    @Qualifier("googleRestClient")
    RestClient googleRestClient(DespachoProperties props) {
        var google = props.distancias().google();
        return RestClient.builder()
                .baseUrl(google.baseUrl())
                .requestFactory(requestFactory(google.connectTimeout(), google.readTimeout()))
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
