package utnfc.isi.back.logistica.pedidos.client;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Cliente HTTP hacia despacho-service. Esqueleto: sin llamadas reales todavia.
 */
@Component
public class DespachoClient {

    private final RestClient despachoRestClient;

    public DespachoClient(RestClient despachoRestClient) {
        this.despachoRestClient = despachoRestClient;
    }

    protected RestClient cliente() {
        return despachoRestClient;
    }
}
