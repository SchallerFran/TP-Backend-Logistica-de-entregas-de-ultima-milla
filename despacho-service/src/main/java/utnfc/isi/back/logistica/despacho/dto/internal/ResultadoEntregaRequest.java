package utnfc.isi.back.logistica.despacho.dto.internal;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Resultado de la parada informado por el repartidor.
 * entregado=true → ENTREGADO. entregado=false → FALLIDO y luego, automáticamente en pedidos,
 * RECIBIDO (si quedan intentos) o DEVUELTO (segundo intento fallido).
 */
public record ResultadoEntregaRequest(
        @NotNull Long rutaId,
        boolean entregado,
        @Size(max = 255) String motivo
) {
}
