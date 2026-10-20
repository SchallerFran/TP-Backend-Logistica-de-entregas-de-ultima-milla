package utnfc.isi.back.logistica.despacho.client;

import utnfc.isi.back.logistica.despacho.dto.internal.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.util.List;
import java.util.function.Supplier;

/**
 * Cliente de la API interna de pedidos-service (contrato en el paquete common.dto.internal).
 * <p>
 * Traduce los fallos en dos categorías, porque quien llama actúa distinto en cada caso:
 * <ul>
 *   <li>{@link PedidosNoDisponibleException}: transitorio (red, timeout, 5xx, 401/403 por token) → reintentar.</li>
 *   <li>{@link PedidosRechazoException}: pedidos dijo que no (404/409/422) → no reintentar.</li>
 * </ul>
 */
@Slf4j
@Component
public class PedidosClient {

    private final RestClient pedidos;

    public PedidosClient(@Qualifier("pedidosRestClient") RestClient pedidosRestClient) {
        this.pedidos = pedidosRestClient;
    }

    public List<PedidoPendienteDTO> obtenerPendientes(Long centroId, LocalDate fecha) {
        return invocar("obtener pendientes", () -> pedidos.get()
                .uri(uri -> uri.path("/internal/pedidos/pendientes")
                        .queryParam("centroId", centroId)
                        .queryParam("fecha", fecha)
                        .build())
                .retrieve()
                .body(new ParameterizedTypeReference<List<PedidoPendienteDTO>>() {
                }));
    }

    public ConfirmacionAsignacionResponse confirmarAsignacion(ConfirmacionAsignacionRequest request) {
        return invocar("confirmar asignación", () -> pedidos.post()
                .uri("/internal/pedidos/asignaciones")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(ConfirmacionAsignacionResponse.class));
    }

    public void notificarEnCamino(Long pedidoId, InicioEntregaRequest request) {
        invocar("notificar en camino", () -> pedidos.post()
                .uri("/internal/pedidos/{id}/en-camino", pedidoId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toBodilessEntity());
    }

    public ResultadoEntregaResponse notificarResultado(Long pedidoId, ResultadoEntregaRequest request) {
        return invocar("notificar resultado", () -> pedidos.post()
                .uri("/internal/pedidos/{id}/resultado-entrega", pedidoId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(ResultadoEntregaResponse.class));
    }

    private <T> T invocar(String operacion, Supplier<T> llamada) {
        try {
            return llamada.get();
        } catch (HttpClientErrorException e) {
            HttpStatusCode status = e.getStatusCode();
            if (status.value() == 401 || status.value() == 403) {
                // Problema de credenciales entre servicios (config de Keycloak): no es culpa del pedido
                log.error("pedidos-service rechazó el token de servicio en '{}': {}", operacion, status);
                throw new PedidosNoDisponibleException("Error de autenticación entre servicios", e);
            }
            throw new PedidosRechazoException(status.value(),
                    "pedidos-service rechazó '" + operacion + "': " + e.getResponseBodyAsString());
        } catch (RuntimeException e) {
            // ResourceAccessException (red/timeout), HttpServerErrorException (5xx),
            // errores al obtener el token de Keycloak, etc.
            log.warn("pedidos-service no disponible en '{}': {}", operacion, e.getMessage());
            throw new PedidosNoDisponibleException("pedidos-service no disponible", e);
        }
    }
}
