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

/**
 * despacho-service es un resource server: TODA petición trae un JWT de Keycloak.
 * <p>
 * Defensa en profundidad: el gateway ya filtra por rol, pero el servicio vuelve a validar el token y
 * los roles porque dentro de la red de docker alguien podría llamarlo sin pasar por el gateway.
 * <p>
 * Reglas gruesas por URL acá; las reglas por propiedad ("el repartidor sólo su ruta") se controlan
 * en la capa de servicio, porque dependen de datos.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    /** Conversor de roles de Keycloak del JAR comun (bean via scanBasePackages). */
    private final KeycloakRoleConverter keycloakRoleConverter;

    public SecurityConfig(KeycloakRoleConverter keycloakRoleConverter) {
        this.keycloakRoleConverter = keycloakRoleConverter;
    }

    private static final String API = "/api/despacho";

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // API stateless con bearer token: no hay cookies de sesión → CSRF no aplica
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html",
                                "/actuator/health/**", "/actuator/info").permitAll()

                        // API interna: sólo cuentas de servicio (client credentials)
                        .requestMatchers("/internal/**").hasRole(Roles.SERVICE)

                        // Datos maestros: lectura para despachante y admin, escritura sólo admin
                        .requestMatchers(HttpMethod.GET, API + "/centros/**", API + "/vehiculos/**",
                                API + "/repartidores/**").hasAnyRole(Roles.ADMIN, Roles.DESPACHANTE)
                        .requestMatchers(API + "/centros/**", API + "/vehiculos/**",
                                API + "/repartidores/**").hasRole(Roles.ADMIN)

                        // Repartidor: su ruta del día y la ejecución
                        .requestMatchers(API + "/mi-ruta/**").hasRole(Roles.REPARTIDOR)

                        // Planificación y supervisión
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

    /** realm_access.roles -> ROLE_*, y el principal = claim "sub" (id del usuario en Keycloak). */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(keycloakRoleConverter);
        converter.setPrincipalClaimName("sub");
        return converter;
    }
}
