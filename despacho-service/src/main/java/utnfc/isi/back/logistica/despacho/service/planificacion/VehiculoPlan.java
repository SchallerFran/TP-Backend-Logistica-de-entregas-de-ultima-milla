package utnfc.isi.back.logistica.despacho.service.planificacion;

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
