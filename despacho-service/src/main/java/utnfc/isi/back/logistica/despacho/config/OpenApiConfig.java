package utnfc.isi.back.logistica.despacho.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/** Swagger UI con botón "Authorize" para pegar el access token de Keycloak. */
@Configuration
@OpenAPIDefinition(
        info = @Info(title = "despacho-service",
                version = "1.0.0",
                description = "Operación logística: centros, flota y repartidores; planificación, "
                        + "confirmación y ejecución de rutas; integración con Google Distance Matrix."),
        security = @SecurityRequirement(name = "bearerAuth"))
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
public class OpenApiConfig {
}
