package utnfc.isi.back.logistica.despacho.security;

/**
 * Roles de realm definidos en Keycloak (realm "logistica").
 * Se centralizan para que gateway y servicios usen exactamente los mismos nombres.
 */
public final class Roles {

    /** Comercio que carga pedidos. Sólo ve y opera sus propios pedidos. */
    public static final String CLIENTE = "CLIENTE";
    /** Operador que planifica la jornada y supervisa rutas. */
    public static final String DESPACHANTE = "DESPACHANTE";
    /** Conductor: ve e informa sólo su ruta. */
    public static final String REPARTIDOR = "REPARTIDOR";
    /** Mantiene los datos maestros (centros, vehículos, repartidores). */
    public static final String ADMIN = "ADMIN";
    /**
     * Rol de las cuentas de servicio (client credentials). Es el único habilitado para /internal/**.
     * Ningún usuario humano debe tenerlo.
     */
    public static final String SERVICE = "SERVICE";

    private Roles() {
    }
}
