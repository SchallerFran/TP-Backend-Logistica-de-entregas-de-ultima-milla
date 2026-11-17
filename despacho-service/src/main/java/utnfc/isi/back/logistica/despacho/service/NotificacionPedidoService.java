package utnfc.isi.back.logistica.despacho.service;

import utnfc.isi.back.logistica.despacho.dto.internal.InicioEntregaRequest;
import utnfc.isi.back.logistica.despacho.dto.internal.ResultadoEntregaRequest;
import utnfc.isi.back.logistica.despacho.exception.RecursoNoEncontradoException;
import utnfc.isi.back.logistica.despacho.exception.ReglaNegocioException;
import utnfc.isi.back.logistica.despacho.client.PedidosClient;
import utnfc.isi.back.logistica.despacho.client.PedidosNoDisponibleException;
import utnfc.isi.back.logistica.despacho.client.PedidosRechazoException;
import utnfc.isi.back.logistica.despacho.config.DespachoProperties;
import utnfc.isi.back.logistica.despacho.dto.DespachoMapper;
import utnfc.isi.back.logistica.despacho.dto.NotificacionResponse;
import utnfc.isi.back.logistica.despacho.model.EstadoNotificacion;
import utnfc.isi.back.logistica.despacho.model.NotificacionPedido;
import utnfc.isi.back.logistica.despacho.repository.NotificacionPedidoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificacionPedidoService {

    private final NotificacionPedidoRepository repo;
    private final PedidosClient pedidosClient;
    private final DespachoProperties props;
    private final Clock clock;

    private final ReentrantLock lock = new ReentrantLock();

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void alRegistrarse(NotificacionesRegistradasEvent evento) {
        procesarPendientes();
    }

    @Scheduled(fixedDelayString = "${despacho.notificaciones.intervalo-ms}",
            initialDelayString = "${despacho.notificaciones.intervalo-ms}")
    public void reintentoPeriodico() {
        procesarPendientes();
    }

    public void procesarPendientes() {
        if (!lock.tryLock()) {
            return;
        }
        try {
            List<NotificacionPedido> pendientes = repo.findTop100ByEstadoOrderByIdAsc(EstadoNotificacion.PENDIENTE);
            for (NotificacionPedido n : pendientes) {
                if (!enviar(n)) {
                    break;
                }
            }
        } finally {
            lock.unlock();
        }
    }

    private boolean enviar(NotificacionPedido n) {
        n.setIntentos(n.getIntentos() + 1);
        try {
            switch (n.getTipo()) {
                case EN_CAMINO -> pedidosClient.notificarEnCamino(n.getPedidoId(), new InicioEntregaRequest(n.getRutaId()));
                case RESULTADO_ENTREGA -> pedidosClient.notificarResultado(n.getPedidoId(),
                        new ResultadoEntregaRequest(n.getRutaId(), Boolean.TRUE.equals(n.getEntregado()), n.getMotivo()));
            }
            n.setEstado(EstadoNotificacion.ENVIADA);
            n.setEnviadaEn(LocalDateTime.now(clock));
            n.setUltimoError(null);
            repo.save(n);
            return true;
        } catch (PedidosRechazoException e) {
            log.error("pedidos-service rechazó la notificación {} ({} pedido {}): {}",
                    n.getId(), n.getTipo(), n.getPedidoId(), e.getMessage());
            n.setEstado(EstadoNotificacion.ERROR);
            n.setUltimoError(recortar(e.getMessage()));
            repo.save(n);
            return true;
        } catch (PedidosNoDisponibleException e) {
            n.setUltimoError(recortar(e.getMessage()));
            if (n.getIntentos() >= props.notificaciones().maxIntentos()) {
                n.setEstado(EstadoNotificacion.ERROR);
            }
            repo.save(n);
            return false;
        }
    }


    @Transactional(readOnly = true)
    public List<NotificacionResponse> listar(EstadoNotificacion estado) {
        return repo.findByEstadoOrderByIdAsc(estado).stream().map(DespachoMapper::aResponse).toList();
    }

    @Transactional
    public NotificacionResponse reencolar(Long id) {
        NotificacionPedido n = repo.findById(id).orElseThrow(() -> RecursoNoEncontradoException.de("Notificación", id));
        if (n.getEstado() != EstadoNotificacion.ERROR) {
            throw new ReglaNegocioException("NOTIFICACION_NO_ERROR", "Sólo se reencolan notificaciones en ERROR");
        }
        n.setEstado(EstadoNotificacion.PENDIENTE);
        n.setIntentos(0);
        return DespachoMapper.aResponse(n);
    }

    private static String recortar(String s) {
        if (s == null) {
            return null;
        }
        return s.length() > 500 ? s.substring(0, 500) : s;
    }
}
