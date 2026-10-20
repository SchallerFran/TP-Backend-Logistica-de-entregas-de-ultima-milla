package utnfc.isi.back.logistica.despacho.dto;

public record VehiculoResponse(
        Long id,
        String patente,
        double pesoMaxKg,
        double volMaxM3,
        double autonomiaKm,
        double costoPorKm,
        boolean tieneFrio,
        boolean disponible,
        Long centroId
) {
}
