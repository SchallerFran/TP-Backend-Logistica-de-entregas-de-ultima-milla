package utnfc.isi.back.logistica.pedidos.dto;

public record CentroDTO(
        Long id,
        String nombre,
        boolean activo,
        Double latitud,
        Double longitud
) {
}
