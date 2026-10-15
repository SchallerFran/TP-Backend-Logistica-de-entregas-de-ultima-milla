package utnfc.isi.back.logistica.despacho.client;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Cliente HTTP hacia pedidos-service. Esqueleto: sin llamadas reales todavia.
 */
@Component
public class PedidosClient {

    private final RestClient pedidosRestClient;

    public PedidosClient(RestClient pedidosRestClient) {
        this.pedidosRestClient = pedidosRestClient;
    }

    protected RestClient cliente() {
        return pedidosRestClient;
    }
}
