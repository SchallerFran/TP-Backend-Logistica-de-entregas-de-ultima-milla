package utnfc.isi.back.logistica.pedidos.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CrearClienteRequest(
        @NotBlank @Size(max = 120) String razonSocial,
        @NotBlank @Size(max = 13) String cuit,
        @NotBlank @Email @Size(max = 120) String email,
        @Size(max = 30) String telefono
) {
}
