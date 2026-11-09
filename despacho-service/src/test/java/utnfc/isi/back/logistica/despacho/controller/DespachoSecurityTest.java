package utnfc.isi.back.logistica.despacho.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import utnfc.isi.back.logistica.commons.KeycloakRoleConverter;
import utnfc.isi.back.logistica.despacho.config.SecurityConfig;
import utnfc.isi.back.logistica.despacho.service.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Matriz de permisos por rol de despacho-service (SecurityConfig). Los servicios están simulados:
 * "permitido" significa que la seguridad deja pasar (el status no es 401 ni 403).
 */
@WebMvcTest
@Import({SecurityConfig.class, KeycloakRoleConverter.class})
class DespachoSecurityTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private JwtDecoder jwtDecoder;
    @MockitoBean private CentroService centroService;
    @MockitoBean private VehiculoService vehiculoService;
    @MockitoBean private RepartidorService repartidorService;
    @MockitoBean private DistanciaService distanciaService;
    @MockitoBean private EjecucionRutaService ejecucionRutaService;
    @MockitoBean private NotificacionPedidoService notificacionPedidoService;
    @MockitoBean private PlanificacionService planificacionService;
    @MockitoBean private CancelacionService cancelacionService;

    @Test
    @DisplayName("Sin token no se accede a la API: 401")
    void sinTokenNoHayAcceso() throws Exception {
        mockMvc.perform(get("/api/despacho/centros")).andExpect(status().isUnauthorized());
    }

    @ParameterizedTest(name = "{0} {1} con {2} -> {3}")
    @CsvSource({
            // datos maestros: lectura despachante y admin, escritura sólo admin
            "GET,    /api/despacho/centros,                 ADMIN,       permitido",
            "GET,    /api/despacho/vehiculos,               DESPACHANTE, permitido",
            "GET,    /api/despacho/repartidores,            DESPACHANTE, permitido",
            "GET,    /api/despacho/centros,                 CLIENTE,     403",
            "GET,    /api/despacho/vehiculos,               REPARTIDOR,  403",
            "POST,   /api/despacho/centros,                 ADMIN,       permitido",
            "POST,   /api/despacho/centros,                 DESPACHANTE, 403",
            "DELETE, /api/despacho/vehiculos/1,             DESPACHANTE, 403",
            // ejecución: sólo el repartidor
            "GET,    /api/despacho/mi-ruta,                 REPARTIDOR,  permitido",
            "GET,    /api/despacho/mi-ruta,                 DESPACHANTE, 403",
            "POST,   /api/despacho/mi-ruta/1/inicio,        CLIENTE,     403",
            // planificación: sólo el despachante
            "POST,   /api/despacho/planificaciones,         DESPACHANTE, permitido",
            "POST,   /api/despacho/planificaciones,         CLIENTE,     403",
            "POST,   /api/despacho/planificaciones,         ADMIN,       403",
            // supervisión
            "GET,    /api/despacho/rutas,                   ADMIN,       permitido",
            "DELETE, /api/despacho/rutas/1,                 ADMIN,       403",
            "GET,    /api/despacho/notificaciones,          REPARTIDOR,  403",
            // API interna: sólo la cuenta de servicio
            "GET,    /internal/centros/1,                   SERVICE,     permitido",
            "GET,    /internal/centros/1,                   ADMIN,       403",
            "DELETE, /internal/paradas/pedido/1,            DESPACHANTE, 403",
            // la cuenta de servicio no entra a la API pública
            "GET,    /api/despacho/centros,                 SERVICE,     403",
    })
    void matrizDePermisos(String metodo, String url, String rol, String esperado) throws Exception {
        int status = mockMvc.perform(request(HttpMethod.valueOf(metodo), url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .with(jwt().jwt(b -> b.subject("sub-" + rol))
                                .authorities(new SimpleGrantedAuthority("ROLE_" + rol))))
                .andReturn().getResponse().getStatus();

        if ("403".equals(esperado)) {
            assertEquals(403, status);
        } else {
            assertTrue(status != 401 && status != 403, "la seguridad debería permitirlo, status = " + status);
        }
    }
}
