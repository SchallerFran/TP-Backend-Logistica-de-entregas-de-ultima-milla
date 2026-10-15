package utnfc.isi.back.logistica.pedidos.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import utnfc.isi.back.logistica.commons.KeycloakRoleConverter;
import utnfc.isi.back.logistica.pedidos.config.PedidosExceptionHandler;
import utnfc.isi.back.logistica.pedidos.config.SecurityConfig;
import utnfc.isi.back.logistica.pedidos.service.AccesoDenegadoException;
import utnfc.isi.back.logistica.pedidos.service.PedidoService;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {PedidoController.class, PedidoInternoController.class})
@Import({SecurityConfig.class, KeycloakRoleConverter.class, PedidosExceptionHandler.class})
class PedidoControllerSecurityTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private PedidoService pedidoService;
    @MockitoBean private JwtDecoder jwtDecoder;

    @Test
    @DisplayName("Sin token no se accede a la API publica")
    void sinTokenNoHayAcceso() throws Exception {
        mockMvc.perform(get("/api/pedidos/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Un CLIENTE no puede usar los endpoints internos")
    void clienteNoAccedeAInternal() throws Exception {
        mockMvc.perform(get("/internal/pedidos/pendientes?centroId=1")
                        .with(jwt()
                                .jwt(builder -> builder.subject("sub-cliente-1"))
                                .authorities(new SimpleGrantedAuthority("ROLE_CLIENTE"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("La cuenta de servicio si puede usar los endpoints internos")
    void serviceAccedeAInternal() throws Exception {
        when(pedidoService.pendientes(1L)).thenReturn(List.of());

        mockMvc.perform(get("/internal/pedidos/pendientes?centroId=1")
                        .with(jwt()
                                .jwt(builder -> builder.subject("service-account").claim("azp", "logistica-service"))
                                .authorities(new SimpleGrantedAuthority("ROLE_SERVICE"))))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Un CLIENTE no ve el pedido de otro cliente: 403")
    void clienteNoVePedidoAjeno() throws Exception {
        when(pedidoService.obtener(eq(7L), anyString(), eq(true)))
                .thenThrow(AccesoDenegadoException.pedidoAjeno(7L));

        mockMvc.perform(get("/api/pedidos/7")
                        .with(jwt()
                                .jwt(builder -> builder.subject("sub-cliente-1"))
                                .authorities(new SimpleGrantedAuthority("ROLE_CLIENTE"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Un DESPACHANTE ve cualquier pedido: el service recibe soloPropios = false")
    void despachanteVeCualquierPedido() throws Exception {
        when(pedidoService.obtener(anyLong(), anyString(), eq(false))).thenReturn(null);

        mockMvc.perform(get("/api/pedidos/7")
                        .with(jwt()
                                .jwt(builder -> builder.subject("sub-despachante"))
                                .authorities(new SimpleGrantedAuthority("ROLE_DESPACHANTE"))))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("La cuenta de servicio no entra a la API publica de pedidos")
    void serviceNoAccedeALaApiPublica() throws Exception {
        mockMvc.perform(get("/api/pedidos/7")
                        .with(jwt()
                                .jwt(builder -> builder.subject("service-account"))
                                .authorities(new SimpleGrantedAuthority("ROLE_SERVICE"))))
                .andExpect(status().isForbidden());
    }
}
