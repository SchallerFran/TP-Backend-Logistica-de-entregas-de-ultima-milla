package utnfc.isi.back.logistica.despacho.service.planificacion;

/** De dónde salió una distancia de la matriz. */
public enum FuenteDistancia {
    /** Mismo punto: 0 m, 0 s. */
    MISMO_PUNTO,
    /** Caché vigente (consultada a OSRM hace menos del TTL). */
    CACHE,
    /** Consultada a OSRM en esta operación. */
    OSRM,
    /** Caché vencida: OSRM no respondió, pero el dato es real (aunque viejo). */
    CACHE_VENCIDA,
    /** Ni OSRM ni caché: Haversine × factor de rodeo. Dato NO exacto: la ruta queda "estimada". */
    ESTIMADA
}
