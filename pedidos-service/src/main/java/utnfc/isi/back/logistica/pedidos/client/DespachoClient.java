package utnfc.isi.back.logistica.pedidos.client;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import utnfc.isi.back.logistica.pedidos.dto.CentroDTO;
import utnfc.isi.back.logistica.pedidos.service.CentroInexistenteException;
import utnfc.isi.back.logistica.pedidos.service.ServicioNoDisponibleException;

/**
 * Cliente HTTP hacia despacho-service.
 *
 * Ninguna RestClientException sale de esta clase: el GlobalExceptionHandler del
 * JAR comun las mapea todas a 503 y convertiria un 404 de despacho (centro
 * inexistente, que es 422) en un 503 incorrecto.
 */
@Component
public class DespachoClient {

    private final RestClient despachoRestClient;

    public DespachoClient(RestClient despachoRestClient) {
        this.despachoRestClient = despachoRestClient;
    }

    /**
     * GET /internal/centros/{id}
     *
     * @throws CentroInexistenteException   si despacho-service responde 404 (-> 422)
     * @throws ServicioNoDisponibleException si hay timeout, error de conexion o 5xx (-> 503)
     */
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
            // ResourceAccessException (timeout / conexion rechazada) y errores de parseo
            throw new ServicioNoDisponibleException(
                    "No se pudo consultar despacho-service: " + ex.getMessage(), ex);
        }

        if (centro == null) {
            throw new ServicioNoDisponibleException(
                    "despacho-service devolvio una respuesta vacia para el centro " + centroId);
        }
        return centro;
    }
}
