package utnfc.isi.back.logistica.despacho.service;

import utnfc.isi.back.logistica.despacho.exception.RecursoNoEncontradoException;
import utnfc.isi.back.logistica.despacho.exception.ReglaNegocioException;
import utnfc.isi.back.logistica.despacho.dto.DespachoMapper;
import utnfc.isi.back.logistica.despacho.dto.ResultadoParadaRequest;
import utnfc.isi.back.logistica.despacho.dto.RutaResponse;
import utnfc.isi.back.logistica.despacho.model.*;
import utnfc.isi.back.logistica.despacho.repository.NotificacionPedidoRepository;
import utnfc.isi.back.logistica.despacho.repository.RutaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Ejecución en la calle (repartidor) y supervisión (despachante).
 * <p>
 * Nada de esto llama a pedidos-service en forma sincrónica: el cambio en la ruta/parada y la
 * notificación para pedidos se guardan en la MISMA transacción (outbox). Si pedidos-service está caído,
 * el repartidor puede seguir trabajando y los pedidos se ponen al día cuando vuelva.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EjecucionRutaService {

    private final RutaRepository rutaRepository;
    private final NotificacionPedidoRepository notificacionRepository;
    private final ApplicationEventPublisher eventos;
    private final Clock clock;

    // ---------------------------------------------------------------- repartidor

    /** La ruta del repartidor autenticado para la fecha (por defecto, hoy). Sólo rutas ya confirmadas. */
    @Transactional(readOnly = true)
    public RutaResponse miRuta(String keycloakUserId, LocalDate fecha) {
        LocalDate dia = fecha != null ? fecha : LocalDate.now(clock);
        Ruta ruta = rutaRepository.findDelRepartidor(keycloakUserId, dia)
                .filter(r -> r.getEstado() != EstadoRuta.BORRADOR)
                .orElseThrow(() -> new RecursoNoEncontradoException("No tiene una ruta asignada para " + dia));
        return DespachoMapper.aResponse(ruta);
    }

    @Transactional
    public RutaResponse iniciar(String keycloakUserId, Long rutaId) {
        Ruta ruta = rutaDelRepartidor(keycloakUserId, rutaId);
        if (ruta.getEstado() != EstadoRuta.CONFIRMADA) {
            throw new ReglaNegocioException("RUTA_NO_CONFIRMADA",
                    "Sólo se puede iniciar una ruta CONFIRMADA (estado actual: " + ruta.getEstado() + ")");
        }
        LocalDate hoy = LocalDate.now(clock);
        if (ruta.getFecha().isAfter(hoy)) {
            throw new ReglaNegocioException("RUTA_FUTURA", "La ruta es del " + ruta.getFecha() + ", todavía no puede iniciarse");
        }

        ruta.setEstado(EstadoRuta.EN_CURSO);
        LocalDateTime ahora = LocalDateTime.now(clock);
        List<NotificacionPedido> notificaciones = ruta.getParadas().stream()
                .map(p -> NotificacionPedido.enCamino(p.getPedidoId(), ruta.getId(), ahora))
                .toList();
        notificacionRepository.saveAll(notificaciones);
        eventos.publishEvent(new NotificacionesRegistradasEvent(notificaciones.size()));
        log.info("Ruta {} iniciada por repartidor {} ({} paradas)", rutaId, keycloakUserId, ruta.getParadas().size());
        return DespachoMapper.aResponse(ruta);
    }

    @Transactional
    public RutaResponse registrarResultado(String keycloakUserId, Long rutaId, Long paradaId,
                                           ResultadoParadaRequest req) {
        Ruta ruta = rutaDelRepartidor(keycloakUserId, rutaId);
        if (ruta.getEstado() != EstadoRuta.EN_CURSO) {
            throw new ReglaNegocioException("RUTA_NO_EN_CURSO",
                    "La ruta debe estar EN_CURSO para registrar entregas (estado actual: " + ruta.getEstado() + ")");
        }
        Parada parada = ruta.buscarParada(paradaId)
                .orElseThrow(() -> RecursoNoEncontradoException.de("Parada de la ruta " + rutaId, paradaId));
        if (parada.getEstado() != EstadoParada.PENDIENTE) {
            throw new ReglaNegocioException("PARADA_CON_RESULTADO",
                    "La parada ya tiene resultado: " + parada.getEstado());
        }

        boolean entregado = req.resultado() == EstadoParada.ENTREGADA;
        parada.setEstado(req.resultado());
        parada.setMotivoFallo(entregado ? null : req.motivo().trim());

        if (ruta.todasLasParadasConResultado()) {
            ruta.setEstado(EstadoRuta.FINALIZADA);
            log.info("Ruta {} finalizada", rutaId);
        }

        notificacionRepository.save(NotificacionPedido.resultado(parada.getPedidoId(), ruta.getId(), entregado,
                parada.getMotivoFallo(), LocalDateTime.now(clock)));
        eventos.publishEvent(new NotificacionesRegistradasEvent(1));
        return DespachoMapper.aResponse(ruta);
    }

    // ---------------------------------------------------------------- despachante

    @Transactional(readOnly = true)
    public List<RutaResponse> buscar(LocalDate fecha, Long centroId, EstadoRuta estado) {
        LocalDate dia = fecha != null ? fecha : LocalDate.now(clock);
        return rutaRepository.buscar(dia, centroId, estado).stream().map(DespachoMapper::aResponse).toList();
    }

    @Transactional(readOnly = true)
    public RutaResponse obtener(Long rutaId) {
        return DespachoMapper.aResponse(rutaRepository.findConRecursos(rutaId)
                .orElseThrow(() -> RecursoNoEncontradoException.de("Ruta", rutaId)));
    }

    // ---------------------------------------------------------------- auxiliares

    /**
     * Autorización por propiedad: el repartidor sólo opera SU ruta.
     * Se responde 404 (y no 403) si la ruta no existe; 403 si existe pero es de otro.
     */
    private Ruta rutaDelRepartidor(String keycloakUserId, Long rutaId) {
        Ruta ruta = rutaRepository.findConRecursos(rutaId)
                .orElseThrow(() -> RecursoNoEncontradoException.de("Ruta", rutaId));
        if (!ruta.getRepartidor().getKeycloakUserId().equals(keycloakUserId)) {
            throw new AccessDeniedException("La ruta " + rutaId + " no está asignada al usuario autenticado");
        }
        return ruta;
    }
}
