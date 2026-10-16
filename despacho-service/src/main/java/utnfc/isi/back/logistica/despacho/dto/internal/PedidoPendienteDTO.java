package utnfc.isi.back.logistica.despacho.dto.internal;

import utnfc.isi.back.logistica.commons.Prioridad;

import java.time.LocalDateTime;

/**
 * Lo mínimo que despacho necesita de un pedido en estado RECIBIDO para planificarlo.
 * No incluye datos personales (destinatario, dirección): despacho no los necesita para decidir.
 */
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
