package utnfc.isi.back.logistica.despacho.service;

import org.junit.jupiter.api.Test;
import utnfc.isi.back.logistica.commons.Coordenada;
import utnfc.isi.back.logistica.despacho.client.OsrmClient;
import utnfc.isi.back.logistica.despacho.client.OsrmClient.Elemento;
import utnfc.isi.back.logistica.despacho.client.RuteoNoDisponibleException;
import utnfc.isi.back.logistica.despacho.config.DespachoProperties;
import utnfc.isi.back.logistica.despacho.model.DistanciaCache;
import utnfc.isi.back.logistica.despacho.repository.DistanciaCacheRepository;
import utnfc.isi.back.logistica.despacho.service.DistanciaService.ResultadoPar;
import utnfc.isi.back.logistica.despacho.service.planificacion.FuenteDistancia;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

/**
 * Orden de respaldo de distancias: caché vigente → OSRM → caché vencida → estimación.
 * Sin Spring ni base: repositorio y cliente OSRM simulados, reloj fijo.
 */
class DistanciaServiceTest {

    private static final ZoneId ZONA = ZoneId.of("America/Argentina/Cordoba");
    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-11-09T12:00:00Z"), ZONA);
    private static final LocalDateTime AHORA = LocalDateTime.now(RELOJ);
    private static final Coordenada A = Coordenada.of(-31.4135, -64.181);
    private static final Coordenada B = Coordenada.of(-31.44, -64.22);

    private final DistanciaCacheRepository cacheRepository = mock(DistanciaCacheRepository.class);
    private final OsrmClient osrm = mock(OsrmClient.class);
    private final DistanciaService service = new DistanciaService(cacheRepository, osrm,
            new DespachoProperties(null, null, null, null, new DespachoProperties.Distancias(
                    new DespachoProperties.Osrm("https://osrm.test", null, null), 30, 1.3, 30)),
            RELOJ);

    {
        when(osrm.habilitado()).thenReturn(true);
        when(cacheRepository.findByOrigenLatInAndDestinoLatIn(anyCollection(), anyCollection())).thenReturn(List.of());
    }

    @Test
    void conOsrmDisponibleUsaElDatoRealYLoGuardaEnCache() {
        when(osrm.consultar(any(), any())).thenReturn(new Elemento[][]{
                {new Elemento(true, 0, 0, "Ok"), new Elemento(true, 6241, 590, "Ok")},
                {new Elemento(true, 6119, 630, "Ok"), new Elemento(true, 0, 0, "Ok")}});

        ResultadoPar r = service.entre(A, B);

        assertEquals(FuenteDistancia.OSRM, r.fuente());
        assertEquals(6.241, r.km(), 0.001);
        assertEquals(590, r.segundos());
        verify(cacheRepository).saveAll(argThat(lista -> ((List<?>) lista).size() == 2));
    }

    @Test
    void sinOsrmNiCacheEstimaConHaversinePorFactorDeRodeo() {
        when(osrm.consultar(any(), any())).thenThrow(new RuteoNoDisponibleException("caído"));

        ResultadoPar r = service.entre(A, B);

        assertEquals(FuenteDistancia.ESTIMADA, r.fuente());
        double esperadoKm = A.redondeada().distanciaHaversineKm(B.redondeada()) * 1.3;
        assertEquals(esperadoKm, r.km(), 0.001);
        assertEquals(Math.round(esperadoKm / 30 * 3600), r.segundos(), 1);
    }

    @Test
    void conCacheVigenteNoConsultaOsrm() {
        cachear(cache(A, B, 6000, 600, AHORA.minusDays(1)), cache(B, A, 6100, 610, AHORA.minusDays(1)));

        ResultadoPar r = service.entre(A, B);

        assertEquals(FuenteDistancia.CACHE, r.fuente());
        assertEquals(6.0, r.km(), 0.001);
        verify(osrm, never()).consultar(any(), any());
    }

    @Test
    void conCacheVencidaYOsrmCaidoUsaElDatoViejo() {
        cachear(cache(A, B, 6000, 600, AHORA.minusDays(45)));
        when(osrm.consultar(any(), any())).thenThrow(new RuteoNoDisponibleException("caído"));

        ResultadoPar r = service.entre(A, B);

        assertEquals(FuenteDistancia.CACHE_VENCIDA, r.fuente());
        assertEquals(6.0, r.km(), 0.001);
        assertEquals(600, r.segundos());
    }

    @Test
    void siOsrmNoEncuentraRutaEseParSeEstima() {
        when(osrm.consultar(any(), any())).thenReturn(new Elemento[][]{
                {new Elemento(true, 0, 0, "Ok"), new Elemento(false, 0, 0, "SIN_RUTA")},
                {new Elemento(true, 6119, 630, "Ok"), new Elemento(true, 0, 0, "Ok")}});

        var m = service.matriz(List.of(A, B));

        assertEquals(FuenteDistancia.ESTIMADA, m.fuente(A, B));
        assertEquals(FuenteDistancia.OSRM, m.fuente(B, A));
    }

    private void cachear(DistanciaCache... registros) {
        when(cacheRepository.findByOrigenLatInAndDestinoLatIn(anyCollection(), anyCollection())).thenReturn(List.of(registros));
    }

    private static DistanciaCache cache(Coordenada o, Coordenada d, int metros, int segundos, LocalDateTime fecha) {
        DistanciaCache c = new DistanciaCache();
        c.setOrigenLat(o.latitud());
        c.setOrigenLng(o.longitud());
        c.setDestinoLat(d.latitud());
        c.setDestinoLng(d.longitud());
        c.setMetros(metros);
        c.setSegundos(segundos);
        c.setFecha(fecha);
        return c;
    }
}
