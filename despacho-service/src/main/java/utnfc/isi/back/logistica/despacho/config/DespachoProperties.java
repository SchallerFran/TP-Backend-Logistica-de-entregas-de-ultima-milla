package utnfc.isi.back.logistica.despacho.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/** Parámetros propios del servicio (prefijo "despacho" en application.yml). */
@ConfigurationProperties(prefix = "despacho")
public record DespachoProperties(
        String zonaHoraria,
        Planificacion planificacion,
        Pedidos pedidos,
        Notificaciones notificaciones,
        Distancias distancias
) {

    public record Planificacion(int tiempoServicioMin) {
    }

    public record Pedidos(String baseUrl, Duration connectTimeout, Duration readTimeout) {
    }

    public record Notificaciones(long intervaloMs, int maxIntentos) {
    }

    public record Distancias(Osrm osrm, int cacheTtlDias, double factorRodeo, double velocidadPromedioKmh) {
    }

    public record Osrm(String baseUrl, Duration connectTimeout, Duration readTimeout) {

        public boolean habilitado() {
            return baseUrl != null && !baseUrl.isBlank();
        }
    }
}
