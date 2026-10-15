package utnfc.isi.back.logistica.pedidos.dto;

import jakarta.validation.constraints.Size;

/** Body de /cancelacion y /reprogramacion. */
public record MotivoRequest(
        @Size(max = 255) String motivo
) {
}
