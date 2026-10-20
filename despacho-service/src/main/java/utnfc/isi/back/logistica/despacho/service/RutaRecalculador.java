package utnfc.isi.back.logistica.despacho.service;

import utnfc.isi.back.logistica.despacho.dto.internal.PedidoPendienteDTO;
import utnfc.isi.back.logistica.despacho.dto.internal.Prioridad;
import utnfc.isi.back.logistica.commons.Coordenada;
import utnfc.isi.back.logistica.despacho.config.DespachoProperties;
import utnfc.isi.back.logistica.despacho.model.Parada;
import utnfc.isi.back.logistica.despacho.model.Repartidor;
import utnfc.isi.back.logistica.despacho.model.Ruta;
import utnfc.isi.back.logistica.despacho.model.Vehiculo;
import utnfc.isi.back.logistica.despacho.repository.RutaRepository;
import utnfc.isi.back.logistica.despacho.service.planificacion.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Operaciones sobre una ruta ya persistida que cambia de composición (pedidos rechazados en la
 * confirmación, pedidos cancelados antes de salir): quitar paradas y recalcular métricas y ETAs.
 * Debe llamarse dentro de una transacción.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RutaRecalculador {

    private final RutaRepository rutaRepository;
    private final DistanciaService distanciaService;
    private final CalculadoraRuta calculadora;
    private final DespachoProperties props;

    /**
     * Quita de la ruta las paradas de los pedidos indicados. Si la ruta queda vacía la elimina.
     *
     * @param matriz matriz que ya contiene los puntos de la ruta, o null para obtenerla
     * @return true si la ruta sigue existiendo
     */
    public boolean quitarParadas(Ruta ruta, Collection<Long> pedidoIds, MatrizDistancias matriz) {
        boolean cambio = ruta.getParadas().removeIf(p -> pedidoIds.contains(p.getPedidoId()));
        if (!cambio) {
            return true;
        }
        if (ruta.getParadas().isEmpty()) {
            rutaRepository.delete(ruta);
            rutaRepository.flush();
            return false;
        }
        // Primero se ejecutan los DELETE de las paradas quitadas y recién después se renumera:
        // si no, el UPDATE de "orden" chocaría con la restricción única (ruta_id, orden).
        rutaRepository.flush();
        ruta.renumerar();
        recalcular(ruta, matriz);
        return true;
    }

    /** Recalcula km, duración, costo, ETAs y el flag estimada. Si matriz es null la obtiene (normalmente de caché). */
    public void recalcular(Ruta ruta, MatrizDistancias matriz) {
        Vehiculo v = ruta.getVehiculo();
        Repartidor r = ruta.getRepartidor();
        Coordenada centro = ruta.getCentro().coordenada();

        List<PedidoPendienteDTO> secuencia = ruta.getParadas().stream().map(RutaRecalculador::comoPedido).toList();
        if (matriz == null) {
            List<Coordenada> puntos = new ArrayList<>();
            puntos.add(centro);
            ruta.getParadas().forEach(p -> puntos.add(p.coordenada()));
            matriz = distanciaService.matriz(puntos);
        }

        ParametrosPlan params = new ParametrosPlan(ruta.getFecha(), centro, ruta.getCentro().getRadioCoberturaKm(),
                props.planificacion().tiempoServicioMin());
        EvaluacionRuta ev = calculadora.evaluar(secuencia,
                new VehiculoPlan(v.getId(), v.getPatente(), v.getPesoMaxKg(), v.getVolMaxM3(), v.getAutonomiaKm(),
                        v.getCostoPorKm(), v.isTieneFrio()),
                new RepartidorPlan(r.getId(), r.getNombre(), r.getTurnoInicio(), r.getTurnoFin()),
                params, matriz);
        if (!ev.factible()) {
            // Al quitar paradas no debería pasar (sólo se adelantan llegadas); puede ocurrir si cambiaron las distancias
            log.warn("La ruta {} quedó con una restricción violada al recalcular: {}", ruta.getId(), ev.violacion());
        }

        ruta.setKmTotal(ev.kmTotal());
        ruta.setDuracionMin(ev.duracionMin());
        ruta.setCostoTotal(ev.kmTotal() * v.getCostoPorKm());
        ruta.setEstimada(ev.estimada());
        for (int i = 0; i < ruta.getParadas().size(); i++) {
            ruta.getParadas().get(i).setEtaEstimada(ev.etas().get(i));
        }
    }

    /** La parada guarda una copia del pedido; se la adapta a la vista que usa la calculadora. */
    private static PedidoPendienteDTO comoPedido(Parada p) {
        return new PedidoPendienteDTO(p.getPedidoId(), null, p.getLatitud(), p.getLongitud(), p.getPesoKg(),
                p.getVolumenM3(), Prioridad.NORMAL, p.getVentanaDesde(), p.getVentanaHasta(), false, 0);
    }
}
