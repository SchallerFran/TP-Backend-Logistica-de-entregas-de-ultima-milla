package utnfc.isi.back.logistica.despacho.repository;

import utnfc.isi.back.logistica.despacho.model.EstadoNotificacion;
import utnfc.isi.back.logistica.despacho.model.NotificacionPedido;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificacionPedidoRepository extends JpaRepository<NotificacionPedido, Long> {

    /** Pendientes en orden de creación: respeta el orden EN_CAMINO → RESULTADO de cada pedido. */
    List<NotificacionPedido> findTop100ByEstadoOrderByIdAsc(EstadoNotificacion estado);

    List<NotificacionPedido> findByEstadoOrderByIdAsc(EstadoNotificacion estado);

    long countByEstado(EstadoNotificacion estado);
}
