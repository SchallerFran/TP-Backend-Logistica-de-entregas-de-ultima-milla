package utnfc.isi.back.logistica.despacho.dto.internal;

public record CentroInternoDTO(
        Long id,
        String nombre,
        double latitud,
        double longitud,
        double radioCoberturaKm,
        boolean activo
) {
}
