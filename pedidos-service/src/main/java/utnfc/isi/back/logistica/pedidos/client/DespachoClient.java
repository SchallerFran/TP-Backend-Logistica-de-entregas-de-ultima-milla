package utnfc.isi.back.logistica.pedidos.client;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import utnfc.isi.back.logistica.pedidos.dto.CentroDTO;
import utnfc.isi.back.logistica.pedidos.service.CentroInexistenteException;
import utnfc.isi.back.logistica.pedidos.service.ServicioNoDisponibleException;

@Component
public class DespachoClient {

    private final RestClient despachoRestClient;

    public DespachoClient(RestClient despachoRestClient) {
        this.despachoRestClient = despachoRestClient;
    }

    public CentroDTO obtenerCentro(Long centroId) {
        CentroDTO centro;
        try {
            centro = despachoRestClient.get()
                    .uri("/internal/centros/{id}", centroId)
                    .retrieve()
                    .onStatus(status -> status.value() == HttpStatus.NOT_FOUND.value(),
                            (request, response) -> {
                                throw new CentroInexistenteException(centroId);
                            })
                    .onStatus(HttpStatusCode::isError,
                            (request, response) -> {
                                throw new ServicioNoDisponibleException(
                                        "despacho-service respondio " + response.getStatusCode()
                                                + " al consultar el centro " + centroId);
                            })
                    .body(CentroDTO.class);
        } catch (CentroInexistenteException | ServicioNoDisponibleException ex) {
            throw ex;
        } catch (RestClientException ex) {
            throw new ServicioNoDisponibleException(
                    "No se pudo consultar despacho-service: " + ex.getMessage(), ex);
        }

        if (centro == null) {
            throw new ServicioNoDisponibleException(
                    "despacho-service devolvio una respuesta vacia para el centro " + centroId);
        }
        return centro;
    }

    public void quitarDeRuta(Long pedidoId) {
        try {
            despachoRestClient.delete()
                    .uri("/internal/paradas/pedido/{id}", pedidoId)
                    .retrieve()
                    .onStatus(status -> status.value() == HttpStatus.CONFLICT.value(),
                            (request, response) -> {
                                throw new IllegalArgumentException(
                                        "El pedido " + pedidoId + " ya salio a la calle y no puede cancelarse");
                            })
                    .onStatus(HttpStatusCode::isError,
                            (request, response) -> {
                                throw new ServicioNoDisponibleException(
                                        "despacho-service respondio " + response.getStatusCode()
                                                + " al quitar el pedido " + pedidoId + " de su ruta");
                            })
                    .toBodilessEntity();
        } catch (RestClientException ex) {
            throw new ServicioNoDisponibleException(
                    "No se pudo consultar despacho-service: " + ex.getMessage(), ex);
        }
    }
}
