package utnfc.isi.back.logistica.pedidos.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Resultado de una planificacion que despacho-service confirma en una sola llamada:
 * los pedidos de las rutas pasan a ASIGNADO y los no asignables a NO_ASIGNABLE.
 */
public record ConfirmacionAsignacionRequest(
        @NotNull @Valid List<Ruta> rutas,
        @NotNull @Valid List<NoAsignable> noAsignables
) {
    public record Ruta(@NotNull Long rutaId, @NotEmpty List<Long> pedidoIds) {
    }

    /** codigo: motivo estable (ej. FUERA_DE_COBERTURA); detalle: texto para el historial. */
    public record NoAsignable(@NotNull Long pedidoId, @NotBlank String codigo, String detalle) {
    }
}
