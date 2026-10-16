package utnfc.isi.back.logistica.despacho.service;

import utnfc.isi.back.logistica.despacho.geo.Coordenada;
import utnfc.isi.back.logistica.despacho.client.GoogleDistanceMatrixClient;
import utnfc.isi.back.logistica.despacho.client.GoogleDistanceMatrixClient.Elemento;
import utnfc.isi.back.logistica.despacho.client.GoogleNoDisponibleException;
import utnfc.isi.back.logistica.despacho.config.DespachoProperties;
import utnfc.isi.back.logistica.despacho.model.DistanciaCache;
import utnfc.isi.back.logistica.despacho.repository.DistanciaCacheRepository;
import utnfc.isi.back.logistica.despacho.service.planificacion.FuenteDistancia;
import utnfc.isi.back.logistica.despacho.service.planificacion.MatrizDistancias;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Resuelve distancias y tiempos de viaje reales entre puntos.
 * <p>
 * Estrategia, en orden, para cada par origen → destino:
 * <ol>
 *   <li><b>Caché vigente</b> (distancia_cache, más nueva que el TTL): no se consulta Google.</li>
 *   <li><b>Google Distance Matrix</b>, en bloques de 10 × 10, sólo para los pares que faltan.
 *       Lo obtenido se guarda en la caché.</li>
 *   <li><b>Caché vencida</b>: si Google no responde/rechaza, se reutiliza el dato viejo (es real).</li>
 *   <li><b>Estimación</b>: Haversine × factor de rodeo y velocidad promedio. El par queda marcado
 *       ESTIMADA y la ruta que lo use, {@code estimada = true}, para que el despachante la revise.</li>
 * </ol>
 * Si Google falla en un bloque, no se lo vuelve a llamar en el resto de la operación ("corta el
 * circuito" localmente): insistir contra una API caída o sin cuota sólo agrega latencia.
 */
@Slf4j
@Service
public class DistanciaService {

    private final DistanciaCacheRepository cacheRepository;
    private final GoogleDistanceMatrixClient google;
    private final DespachoProperties.Distancias config;
    private final Clock clock;

    public DistanciaService(DistanciaCacheRepository cacheRepository, GoogleDistanceMatrixClient google,
                            DespachoProperties props, Clock clock) {
        this.cacheRepository = cacheRepository;
        this.google = google;
        this.config = props.distancias();
        this.clock = clock;
    }

    /** Distancia entre dos puntos (para el endpoint de consulta). */
    public ResultadoPar entre(Coordenada origen, Coordenada destino) {
        MatrizDistancias m = matriz(List.of(origen, destino));
        return new ResultadoPar(m.km(origen, destino), m.segundos(origen, destino), m.fuente(origen, destino));
    }

    public record ResultadoPar(double km, int segundos, FuenteDistancia fuente) {
    }

    /** Matriz completa (todos contra todos) para el conjunto de puntos. */
    public MatrizDistancias matriz(Collection<Coordenada> puntosOriginales) {
        List<Coordenada> puntos = new ArrayList<>(new LinkedHashSet<>(
                puntosOriginales.stream().map(Coordenada::redondeada).toList()));
        int n = puntos.size();
        int[][] metros = new int[n][n];
        int[][] segundos = new int[n][n];
        FuenteDistancia[][] fuente = new FuenteDistancia[n][n];
        LocalDateTime ahora = LocalDateTime.now(clock);
        LocalDateTime limiteVigencia = ahora.minusDays(config.cacheTtlDias());

        // 1) caché (una sola consulta) ------------------------------------------------------------
        Map<String, DistanciaCache> cache = cargarCache(puntos);
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (i == j) {
                    fuente[i][j] = FuenteDistancia.MISMO_PUNTO;
                    continue;
                }
                DistanciaCache c = cache.get(clave(puntos.get(i), puntos.get(j)));
                if (c != null && c.getFecha().isAfter(limiteVigencia)) {
                    metros[i][j] = c.getMetros();
                    segundos[i][j] = c.getSegundos();
                    fuente[i][j] = FuenteDistancia.CACHE;
                }
            }
        }

        // 2) Google para lo que falta ---------------------------------------------------------------
        if (hayFaltantes(fuente) && google.habilitado()) {
            consultarGoogle(puntos, metros, segundos, fuente, cache, ahora);
        }

        // 3) y 4) fallback --------------------------------------------------------------------------
        int estimadas = 0;
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (fuente[i][j] != null) {
                    continue;
                }
                DistanciaCache vieja = cache.get(clave(puntos.get(i), puntos.get(j)));
                if (vieja != null) {
                    metros[i][j] = vieja.getMetros();
                    segundos[i][j] = vieja.getSegundos();
                    fuente[i][j] = FuenteDistancia.CACHE_VENCIDA;
                } else {
                    double km = puntos.get(i).distanciaHaversineKm(puntos.get(j)) * config.factorRodeo();
                    metros[i][j] = (int) Math.round(km * 1000);
                    segundos[i][j] = (int) Math.round(km / config.velocidadPromedioKmh() * 3600);
                    fuente[i][j] = FuenteDistancia.ESTIMADA;
                    estimadas++;
                }
            }
        }
        if (estimadas > 0) {
            log.warn("{} distancias estimadas con Haversine (Google no disponible y sin caché)", estimadas);
        }
        return new MatrizDistancias(puntos, metros, segundos, fuente);
    }

    // -----------------------------------------------------------------------------------------------

    private void consultarGoogle(List<Coordenada> puntos, int[][] metros, int[][] segundos,
                                 FuenteDistancia[][] fuente, Map<String, DistanciaCache> cache,
                                 LocalDateTime ahora) {
        int n = puntos.size();
        int bloque = GoogleDistanceMatrixClient.MAX_POR_LADO;
        List<DistanciaCache> aGuardar = new ArrayList<>();

        externo:
        for (int oi = 0; oi < n; oi += bloque) {
            for (int dj = 0; dj < n; dj += bloque) {
                int oFin = Math.min(oi + bloque, n);
                int dFin = Math.min(dj + bloque, n);
                if (!faltaAlguno(fuente, oi, oFin, dj, dFin)) {
                    continue;
                }
                Elemento[][] res;
                try {
                    res = google.consultar(puntos.subList(oi, oFin), puntos.subList(dj, dFin));
                } catch (GoogleNoDisponibleException e) {
                    log.warn("Google Distance Matrix no disponible, se usa fallback: {}", e.getMessage());
                    break externo; // no insistir en esta operación
                }
                for (int i = oi; i < oFin; i++) {
                    for (int j = dj; j < dFin; j++) {
                        if (fuente[i][j] != null) {
                            continue; // ya resuelto (caché vigente o mismo punto)
                        }
                        Elemento e = res[i - oi][j - dj];
                        if (!e.ok()) {
                            log.info("Google sin ruta {} → {} (status {})", puntos.get(i), puntos.get(j), e.status());
                            continue; // queda para fallback
                        }
                        metros[i][j] = e.metros();
                        segundos[i][j] = e.segundos();
                        fuente[i][j] = FuenteDistancia.GOOGLE;
                        aGuardar.add(actualizarCache(cache, puntos.get(i), puntos.get(j), e, ahora));
                    }
                }
            }
        }
        guardarCache(aGuardar);
    }

    private DistanciaCache actualizarCache(Map<String, DistanciaCache> cache, Coordenada o, Coordenada d,
                                           Elemento e, LocalDateTime ahora) {
        DistanciaCache c = cache.computeIfAbsent(clave(o, d), k -> {
            DistanciaCache nueva = new DistanciaCache();
            nueva.setOrigenLat(o.latitud());
            nueva.setOrigenLng(o.longitud());
            nueva.setDestinoLat(d.latitud());
            nueva.setDestinoLng(d.longitud());
            return nueva;
        });
        c.setMetros(e.metros());
        c.setSegundos(e.segundos());
        c.setFecha(ahora);
        return c;
    }

    private void guardarCache(List<DistanciaCache> registros) {
        if (registros.isEmpty()) {
            return;
        }
        try {
            cacheRepository.saveAll(registros);
        } catch (DataIntegrityViolationException e) {
            // Otra planificación concurrente guardó el mismo par: no es un error, la caché es best-effort
            log.debug("Conflicto guardando caché de distancias (concurrencia): {}", e.getMessage());
        }
    }

    private Map<String, DistanciaCache> cargarCache(List<Coordenada> puntos) {
        Set<Double> lats = new HashSet<>();
        puntos.forEach(p -> lats.add(p.latitud()));
        Set<String> clavesBuscadas = new HashSet<>();
        for (Coordenada o : puntos) {
            for (Coordenada d : puntos) {
                clavesBuscadas.add(clave(o, d));
            }
        }
        Map<String, DistanciaCache> mapa = new HashMap<>();
        for (DistanciaCache c : cacheRepository.findByOrigenLatInAndDestinoLatIn(lats, lats)) {
            String k = clave(c.getOrigenLat(), c.getOrigenLng(), c.getDestinoLat(), c.getDestinoLng());
            if (clavesBuscadas.contains(k)) {
                mapa.put(k, c);
            }
        }
        return mapa;
    }

    private static boolean hayFaltantes(FuenteDistancia[][] fuente) {
        return faltaAlguno(fuente, 0, fuente.length, 0, fuente.length);
    }

    private static boolean faltaAlguno(FuenteDistancia[][] fuente, int oi, int oFin, int dj, int dFin) {
        for (int i = oi; i < oFin; i++) {
            for (int j = dj; j < dFin; j++) {
                if (fuente[i][j] == null) {
                    return true;
                }
            }
        }
        return false;
    }

    private static String clave(Coordenada o, Coordenada d) {
        return clave(o.latitud(), o.longitud(), d.latitud(), d.longitud());
    }

    private static String clave(double oLat, double oLng, double dLat, double dLng) {
        return oLat + "|" + oLng + "|" + dLat + "|" + dLng;
    }
}
