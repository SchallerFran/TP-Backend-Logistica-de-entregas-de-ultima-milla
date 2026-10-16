package utnfc.isi.back.logistica.despacho.dto;

public record CentroResponse(
        Long id,
        String nombre,
        String direccion,
        double latitud,
        double longitud,
        double radioCoberturaKm,
        boolean activo
) {
}
