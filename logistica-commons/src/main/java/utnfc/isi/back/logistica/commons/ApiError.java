package utnfc.isi.back.logistica.commons;

import java.time.OffsetDateTime;

/**
 * Cuerpo uniforme de error para todos los microservicios.
 */
public record ApiError(
        OffsetDateTime timestamp,
        int status,
        String error,
        String mensaje,
        String path
) {
    public static ApiError of(int status, String error, String mensaje, String path) {
        return new ApiError(OffsetDateTime.now(), status, error, mensaje, path);
    }
}
