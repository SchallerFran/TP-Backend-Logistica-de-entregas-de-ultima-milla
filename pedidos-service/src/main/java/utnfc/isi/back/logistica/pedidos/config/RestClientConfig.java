package utnfc.isi.back.logistica.pedidos.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.client.OAuth2ClientHttpRequestInterceptor;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * RestClient hacia despacho-service con client_credentials.
 *
 * El registro OAuth2 se arma a mano desde las propiedades keycloak.* que ya
 * existen en application.yml (en vez de spring.security.oauth2.client.*), y el
 * token lo gestiona y cachea Spring Security (OAuth2AuthorizedClientManager).
 */
@Configuration
public class RestClientConfig {

    public static final String REGISTRO_DESPACHO = "despacho";

    private static final Duration TIMEOUT_CONEXION = Duration.ofSeconds(2);
    private static final Duration TIMEOUT_LECTURA = Duration.ofSeconds(3);

    /** Principal fijo: el token es de la aplicacion, no del usuario que dispara la llamada. */
    private static final Authentication PRINCIPAL_SERVICIO = new AnonymousAuthenticationToken(
            "pedidos-service", "pedidos-service", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS"));

    @Bean
    public ClientRegistrationRepository clientRegistrationRepository(
            @Value("${keycloak.token-uri}") String tokenUri,
            @Value("${keycloak.service-client-id}") String clientId,
            @Value("${keycloak.service-client-secret}") String clientSecret) {
        ClientRegistration despacho = ClientRegistration.withRegistrationId(REGISTRO_DESPACHO)
                .tokenUri(tokenUri)
                .clientId(clientId)
                .clientSecret(clientSecret)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .build();
        return new InMemoryClientRegistrationRepository(despacho);
    }

    @Bean
    public OAuth2AuthorizedClientService authorizedClientService(ClientRegistrationRepository repositorio) {
        return new InMemoryOAuth2AuthorizedClientService(repositorio);
    }

    @Bean
    public OAuth2AuthorizedClientManager authorizedClientManager(ClientRegistrationRepository repositorio,
                                                                 OAuth2AuthorizedClientService servicio) {
        AuthorizedClientServiceOAuth2AuthorizedClientManager manager =
                new AuthorizedClientServiceOAuth2AuthorizedClientManager(repositorio, servicio);
        manager.setAuthorizedClientProvider(
                OAuth2AuthorizedClientProviderBuilder.builder().clientCredentials().build());
        return manager;
    }

    @Bean
    public RestClient despachoRestClient(RestClient.Builder builder,
                                         @Value("${servicios.despacho.url}") String despachoUrl,
                                         OAuth2AuthorizedClientManager authorizedClientManager) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(TIMEOUT_CONEXION);
        requestFactory.setReadTimeout(TIMEOUT_LECTURA);

        OAuth2ClientHttpRequestInterceptor interceptor =
                new OAuth2ClientHttpRequestInterceptor(authorizedClientManager);
        interceptor.setClientRegistrationIdResolver(request -> REGISTRO_DESPACHO);
        interceptor.setPrincipalResolver(request -> PRINCIPAL_SERVICIO);

        return builder.baseUrl(despachoUrl)
                .requestFactory(requestFactory)
                .requestInterceptor(interceptor)
                .build();
    }
}
