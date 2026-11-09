package utnfc.isi.back.logistica.pedidos.model;

import jakarta.persistence.PostPersist;
import org.springframework.context.ApplicationEventPublisher;

/**
 * Listener JPA de CambioEstado. Todo cambio de estado (incluida el alta) inserta una fila en
 * cambio_estado, así que este es el único punto donde enterarse de todos. Sólo publica un evento:
 * qué hacer con él (mandar un mail) lo decide NotificadorMail, después del commit.
 * <p>
 * Hibernate pide la instancia a Spring (SpringBeanContainer), por eso puede recibir dependencias.
 */
public class CambioEstadoListener {

    /** Datos del cambio, sin entidades: el evento se consume en otro hilo, fuera de esta sesión. */
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
