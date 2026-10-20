package utnfc.isi.back.logistica.commons;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CoordenadaTest {

    @Test
    void haversineCordobaRosario() {
        Coordenada cordoba = Coordenada.of(-31.4135, -64.18105);
        Coordenada rosario = Coordenada.of(-32.9471, -60.6985);
        double km = cordoba.distanciaHaversineKm(rosario);
        // ~ 368 km en línea recta
        assertTrue(km > 360 && km < 375, "km=" + km);
    }

    @Test
    void distanciaAlMismoPuntoEsCero() {
        Coordenada c = Coordenada.of(-31.4, -64.2);
        assertEquals(0.0, c.distanciaHaversineKm(c), 1e-9);
    }

    @Test
    void redondeaACuatroDecimales() {
        Coordenada c = Coordenada.of(-31.416789, -64.183249).redondeada();
        assertEquals(-31.4168, c.latitud(), 1e-12);
        assertEquals(-64.1832, c.longitud(), 1e-12);
    }

    @Test
    void rechazaRangosInvalidos() {
        assertThrows(IllegalArgumentException.class, () -> Coordenada.of(91, 0));
        assertThrows(IllegalArgumentException.class, () -> Coordenada.of(0, -181));
    }

    @Test
    void parseaLatLng() {
        Coordenada c = Coordenada.parse("-31.4167, -64.1833");
        assertEquals(-31.4167, c.latitud(), 1e-12);
        assertEquals(-64.1833, c.longitud(), 1e-12);
        assertThrows(IllegalArgumentException.class, () -> Coordenada.parse("abc"));
    }

    @Test
    void radioDeCobertura() {
        Coordenada centro = Coordenada.of(-31.4135, -64.18105);
        Coordenada cerca = Coordenada.of(-31.42, -64.19);
        assertTrue(cerca.estaDentroDelRadio(centro, 5));
        assertFalse(cerca.estaDentroDelRadio(centro, 0.5));
    }
}
