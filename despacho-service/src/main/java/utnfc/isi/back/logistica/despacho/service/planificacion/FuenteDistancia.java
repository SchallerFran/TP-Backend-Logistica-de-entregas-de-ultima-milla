package utnfc.isi.back.logistica.despacho.service.planificacion;

/** De dónde salió una distancia de la matriz. */
public enum FuenteDistancia {
    /** Mismo punto: 0 m, 0 s. */
    MISMO_PUNTO,
    /** Caché vigente (consultada a Google hace menos del TTL). */
    CACHE,
    /** Consultada a Google en esta operación. */
    GOOGLE,
    /** Caché vencida: Google no respondió, pero el dato es real (aunque viejo). */
    CACHE_VENCIDA,
    /** Ni Google ni caché: Haversine × factor de rodeo. Dato NO exacto: la ruta queda "estimada". */
    ESTIMADA
}
