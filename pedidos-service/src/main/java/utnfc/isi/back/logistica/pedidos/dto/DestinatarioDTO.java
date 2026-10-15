package utnfc.isi.back.logistica.pedidos.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import utnfc.isi.back.logistica.pedidos.model.Destinatario;

public record DestinatarioDTO(
        @NotBlank @Size(max = 120) String nombre,
        @Size(max = 30) String telefono,
        @NotBlank @Size(max = 200) String direccion,
        @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") Double latitud,
        @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") Double longitud,
        @Size(max = 255) String referencias
) {
    public static DestinatarioDTO from(Destinatario destinatario) {
        if (destinatario == null) {
            return null;
        }
        return new DestinatarioDTO(
                destinatario.getNombre(),
                destinatario.getTelefono(),
                destinatario.getDireccion(),
                destinatario.getLatitud(),
                destinatario.getLongitud(),
                destinatario.getReferencias());
    }
}
