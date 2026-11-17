package utnfc.isi.back.logistica.pedidos.model;

import jakarta.persistence.PostPersist;
import org.springframework.context.ApplicationEventPublisher;

public class CambioEstadoListener {

    public record CambioRegistrado(Long pedidoId, EstadoPedido anterior, EstadoPedido nuevo, String motivo) {
    }

    private final ApplicationEventPublisher eventos;

    public CambioEstadoListener(ApplicationEventPublisher eventos) {
        this.eventos = eventos;
    }

    @PostPersist
    void alRegistrar(CambioEstado cambio) {
        eventos.publishEvent(new CambioRegistrado(cambio.getPedido().getId(),
                cambio.getEstadoAnterior(), cambio.getEstadoNuevo(), cambio.getMotivo()));
    }
}
