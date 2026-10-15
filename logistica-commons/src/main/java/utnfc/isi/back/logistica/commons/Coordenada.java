package utnfc.isi.back.logistica.commons;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

/**
 * Par latitud/longitud con validacion de rango y distancia Haversine.
 */
public record Coordenada(
        @NotNull
        @DecimalMin(value = "-90.0", message = "la latitud debe ser >= -90")
        @DecimalMax(value = "90.0", message = "la latitud debe ser <= 90")
        Double latitud,

        @NotNull
        @DecimalMin(value = "-180.0", message = "la longitud debe ser >= -180")
        @DecimalMax(value = "180.0", message = "la longitud debe ser <= 180")
        Double longitud
) {

    private static final double RADIO_TIERRA_KM = 6371.0;

    public Coordenada {
        if (latitud == null || longitud == null) {
            throw new IllegalArgumentException("latitud y longitud son obligatorias");
        }
        if (latitud < -90.0 || latitud > 90.0) {
            throw new IllegalArgumentException("latitud fuera de rango [-90, 90]: " + latitud);
        }
        if (longitud < -180.0 || longitud > 180.0) {
            throw new IllegalArgumentException("longitud fuera de rango [-180, 180]: " + longitud);
        }
    }

    /**
     * Distancia en linea recta (Haversine) hasta otra coordenada, en kilometros.
     */
    public double distanciaKm(Coordenada otra) {
        if (otra == null) {
            throw new IllegalArgumentException("la coordenada destino es obligatoria");
        }
        double dLat = Math.toRadians(otra.latitud() - this.latitud);
        double dLon = Math.toRadians(otra.longitud() - this.longitud);
        double lat1 = Math.toRadians(this.latitud);
        double lat2 = Math.toRadians(otra.latitud());

        double a = Math.pow(Math.sin(dLat / 2), 2)
                + Math.pow(Math.sin(dLon / 2), 2) * Math.cos(lat1) * Math.cos(lat2);
        double c = 2 * Math.asin(Math.sqrt(a));
        return RADIO_TIERRA_KM * c;
    }
}
