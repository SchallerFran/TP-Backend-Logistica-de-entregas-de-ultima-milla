package utnfc.isi.back.logistica.despacho.dto;

import java.time.LocalTime;

public record RepartidorResponse(
        Long id,
        String nombre,
        String keycloakUserId,
        LocalTime turnoInicio,
        LocalTime turnoFin,
        boolean activo,
        Long centroId
) {
}
