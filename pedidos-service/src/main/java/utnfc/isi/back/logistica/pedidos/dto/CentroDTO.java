package utnfc.isi.back.logistica.pedidos.dto;

/** Contrato de GET /internal/centros/{id} en despacho-service. */
public record CentroDTO(
        Long id,
        String nombre,
        boolean activo,
        Double latitud,
        Double longitud
) {
}
