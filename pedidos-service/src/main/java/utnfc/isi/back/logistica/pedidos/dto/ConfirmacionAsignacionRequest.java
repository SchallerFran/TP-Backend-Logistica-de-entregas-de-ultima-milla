package utnfc.isi.back.logistica.pedidos.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record ConfirmacionAsignacionRequest(
        @NotNull @Valid List<Ruta> rutas,
        @NotNull @Valid List<NoAsignable> noAsignables
) {
    public record Ruta(@NotNull Long rutaId, @NotEmpty List<Long> pedidoIds) {
    }

    public record NoAsignable(@NotNull Long pedidoId, @NotBlank String codigo, String detalle) {
    }
}
