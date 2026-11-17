package utnfc.isi.back.logistica.despacho.dto.internal;

import java.time.LocalDateTime;

public record PedidoPendienteDTO(
        Long id,
        Long centroId,
        double latitud,
        double longitud,
        double pesoKg,
        double volumenM3,
        Prioridad prioridad,
        LocalDateTime ventanaDesde,
        LocalDateTime ventanaHasta,
        boolean requiereFrio,
        int intentos
) {
}
