package utnfc.isi.back.logistica.despacho.service;

import utnfc.isi.back.logistica.despacho.exception.ReglaNegocioException;
import utnfc.isi.back.logistica.despacho.model.EstadoParada;
import utnfc.isi.back.logistica.despacho.model.EstadoRuta;
import utnfc.isi.back.logistica.despacho.model.Ruta;
import utnfc.isi.back.logistica.despacho.repository.RutaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;

/**
 * Cancelación de un pedido ya planificado, invocada por pedidos-service ANTES de pasar el pedido a
 * CANCELADO (API interna). Es la contracara de la confirmación: pedidos es dueño del estado del pedido,
 * despacho es dueño de la ruta.
 * <ul>
 *   <li>Pedido en ruta BORRADOR o CONFIRMADA: se quita la parada y se recalcula la ruta → 204.</li>
 *   <li>Pedido en ruta EN_CURSO con la parada pendiente: ya salió a la calle → 409 (pedidos no debe cancelar).</li>
 *   <li>Pedido en ninguna ruta: no hay nada que hacer → 204 (idempotente).</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CancelacionService {

    private final RutaRepository rutaRepository;
    private final RutaRecalculador recalculador;

    @Transactional
    public void quitarPedido(Long pedidoId) {
        List<Ruta> enCurso = rutaRepository.findConPedidoEnEstados(pedidoId, EnumSet.of(EstadoRuta.EN_CURSO));
        boolean enLaCalle = enCurso.stream()
                .flatMap(r -> r.getParadas().stream())
                .anyMatch(p -> p.getPedidoId().equals(pedidoId) && p.getEstado() == EstadoParada.PENDIENTE);
        if (enLaCalle) {
            throw new ReglaNegocioException("PEDIDO_EN_CAMINO",
                    "El pedido " + pedidoId + " ya salió a la calle y no puede cancelarse");
        }

        List<Ruta> rutas = rutaRepository.findConPedidoEnEstados(pedidoId,
                EnumSet.of(EstadoRuta.BORRADOR, EstadoRuta.CONFIRMADA));
        for (Ruta ruta : rutas) {
            boolean sigue = recalculador.quitarParadas(ruta, List.of(pedidoId), null);
            log.info("Pedido {} quitado de la ruta {} por cancelación{}", pedidoId, ruta.getId(),
                    sigue ? "" : " (la ruta quedó vacía y se eliminó)");
        }
    }
}
