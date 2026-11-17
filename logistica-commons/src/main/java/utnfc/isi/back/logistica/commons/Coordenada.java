package utnfc.isi.back.logistica.commons;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record Coordenada(double latitud, double longitud) {

    private static final double RADIO_TIERRA_KM = 6371.0088;

    public Coordenada {
        if (Double.isNaN(latitud) || latitud < -90 || latitud > 90) {
            throw new IllegalArgumentException("Latitud fuera de rango [-90, 90]: " + latitud);
        }
        if (Double.isNaN(longitud) || longitud < -180 || longitud > 180) {
            throw new IllegalArgumentException("Longitud fuera de rango [-180, 180]: " + longitud);
        }
    }

    public static Coordenada of(double latitud, double longitud) {
        return new Coordenada(latitud, longitud);
    }

    public double distanciaHaversineKm(Coordenada otra) {
        double lat1 = Math.toRadians(this.latitud);
        double lat2 = Math.toRadians(otra.latitud);
        double dLat = lat2 - lat1;
        double dLng = Math.toRadians(otra.longitud - this.longitud);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(lat1) * Math.cos(lat2) * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return RADIO_TIERRA_KM * c;
    }

    public boolean estaDentroDelRadio(Coordenada centro, double radioKm) {
        return centro.distanciaHaversineKm(this) <= radioKm;
    }

    public Coordenada redondeada() {
        return new Coordenada(redondear4(latitud), redondear4(longitud));
    }

    public static double redondear4(double valor) {
        return BigDecimal.valueOf(valor).setScale(4, RoundingMode.HALF_UP).doubleValue();
    }

    public String comoLatLng() {
        return latitud + "," + longitud;
    }

    public static Coordenada parse(String latLng) {
        if (latLng == null) {
            throw new IllegalArgumentException("Coordenada nula");
        }
        String[] partes = latLng.split(",");
        if (partes.length != 2) {
            throw new IllegalArgumentException("Formato esperado 'lat,lng': " + latLng);
        }
        try {
            return new Coordenada(Double.parseDouble(partes[0].trim()), Double.parseDouble(partes[1].trim()));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Formato esperado 'lat,lng': " + latLng, e);
        }
    }
}
