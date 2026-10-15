package utnfc.isi.back.logistica.pedidos.dto;

import utnfc.isi.back.logistica.pedidos.model.Cliente;

import java.time.LocalDateTime;

public record ClienteDTO(
        Long id,
        String keycloakUserId,
        String razonSocial,
        String cuit,
        String email,
        String telefono,
        boolean activo,
        LocalDateTime creadoEn
) {
    public static ClienteDTO from(Cliente cliente) {
        return new ClienteDTO(
                cliente.getId(),
                cliente.getKeycloakUserId(),
                cliente.getRazonSocial(),
                cliente.getCuit(),
                cliente.getEmail(),
                cliente.getTelefono(),
                cliente.isActivo(),
                cliente.getCreadoEn());
    }
}
