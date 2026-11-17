package utnfc.isi.back.logistica.despacho.config;

import utnfc.isi.back.logistica.commons.KeycloakRoleConverter;
import utnfc.isi.back.logistica.despacho.security.Roles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final KeycloakRoleConverter keycloakRoleConverter;

    public SecurityConfig(KeycloakRoleConverter keycloakRoleConverter) {
        this.keycloakRoleConverter = keycloakRoleConverter;
    }

    private static final String API = "/api/despacho";

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html",
                                "/actuator/health/**", "/actuator/info").permitAll()

                        .requestMatchers("/internal/**").hasRole(Roles.SERVICE)

                        .requestMatchers(HttpMethod.GET, API + "/centros/**", API + "/vehiculos/**",
                                API + "/repartidores/**").hasAnyRole(Roles.ADMIN, Roles.DESPACHANTE)
                        .requestMatchers(API + "/centros/**", API + "/vehiculos/**",
                                API + "/repartidores/**").hasRole(Roles.ADMIN)

                        .requestMatchers(API + "/mi-ruta/**").hasRole(Roles.REPARTIDOR)

                        .requestMatchers(API + "/planificaciones/**").hasRole(Roles.DESPACHANTE)
                        .requestMatchers(HttpMethod.GET, API + "/rutas/**").hasAnyRole(Roles.DESPACHANTE, Roles.ADMIN)
                        .requestMatchers(API + "/rutas/**").hasRole(Roles.DESPACHANTE)
                        .requestMatchers(API + "/distancias/**").hasAnyRole(Roles.DESPACHANTE, Roles.ADMIN)
                        .requestMatchers(API + "/notificaciones/**").hasAnyRole(Roles.DESPACHANTE, Roles.ADMIN)

                        .anyRequest().denyAll())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));
        return http.build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(keycloakRoleConverter);
        converter.setPrincipalClaimName("sub");
        return converter;
    }
}
