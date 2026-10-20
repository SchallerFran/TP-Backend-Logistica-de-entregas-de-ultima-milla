package utnfc.isi.back.logistica.despacho.service.planificacion;

/** Vista del vehículo que necesita el planificador (desacoplada de la entidad JPA). */
public record VehiculoPlan(
        Long id,
        String patente,
        double pesoMaxKg,
        double volMaxM3,
        double autonomiaKm,
        double costoPorKm,
        boolean tieneFrio
) {
}
