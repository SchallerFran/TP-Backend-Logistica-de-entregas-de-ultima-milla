package utnfc.isi.back.logistica.despacho.dto;

import utnfc.isi.back.logistica.despacho.dto.internal.CentroInternoDTO;
import utnfc.isi.back.logistica.despacho.model.*;

/** Entidad → DTO. Las entidades nunca salen por los controllers. Llamar dentro de una transacción (LAZY). */
public final class DespachoMapper {

    private DespachoMapper() {
    }

    public static CentroResponse aResponse(CentroDistribucion c) {
        return new CentroResponse(c.getId(), c.getNombre(), c.getDireccion(), c.getLatitud(), c.getLongitud(),
                c.getRadioCoberturaKm(), c.isActivo());
    }

    public static CentroInternoDTO aInterno(CentroDistribucion c) {
        return new CentroInternoDTO(c.getId(), c.getNombre(), c.getLatitud(), c.getLongitud(),
                c.getRadioCoberturaKm(), c.isActivo());
    }

    public static VehiculoResponse aResponse(Vehiculo v) {
        return new VehiculoResponse(v.getId(), v.getPatente(), v.getPesoMaxKg(), v.getVolMaxM3(),
                v.getAutonomiaKm(), v.getCostoPorKm(), v.isTieneFrio(), v.isDisponible(), v.getCentro().getId());
    }

    public static RepartidorResponse aResponse(Repartidor r) {
        return new RepartidorResponse(r.getId(), r.getNombre(), r.getKeycloakUserId(), r.getTurnoInicio(),
                r.getTurnoFin(), r.isActivo(), r.getCentro().getId());
    }

    public static RutaResponse aResponse(Ruta r) {
        return new RutaResponse(r.getId(), r.getFecha(), r.getEstado(), r.getCentro().getId(),
                r.getVehiculo().getId(), r.getVehiculo().getPatente(),
                r.getRepartidor().getId(), r.getRepartidor().getNombre(),
                redondear(r.getKmTotal()), r.getDuracionMin(), redondear(r.getCostoTotal()), r.isEstimada(),
                r.getParadas().stream().map(DespachoMapper::aResponse).toList());
    }

    public static ParadaResponse aResponse(Parada p) {
        return new ParadaResponse(p.getId(), p.getOrden(), p.getPedidoId(), p.getLatitud(), p.getLongitud(),
                p.getPesoKg(), p.getVolumenM3(), p.getVentanaDesde(), p.getVentanaHasta(), p.getEtaEstimada(),
                p.getEstado(), p.getMotivoFallo());
    }

    public static NotificacionResponse aResponse(NotificacionPedido n) {
        return new NotificacionResponse(n.getId(), n.getTipo(), n.getPedidoId(), n.getRutaId(), n.getEntregado(),
                n.getMotivo(), n.getEstado(), n.getIntentos(), n.getUltimoError(), n.getCreadaEn(), n.getEnviadaEn());
    }

    private static double redondear(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
