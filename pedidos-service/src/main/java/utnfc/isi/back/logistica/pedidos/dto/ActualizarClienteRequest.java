package utnfc.isi.back.logistica.pedidos.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ActualizarClienteRequest(
        @NotBlank @Email @Size(max = 120) String email,
        @Size(max = 30) String telefono
) {
}
