package utnfc.isi.back.logistica.despacho.model;

/**
 * Estado del envio, propio de despacho-service. No depende de pedidos-service
 * ni del JAR comun: cada servicio es dueno de su vocabulario.
 */
public enum EstadoEnvio {
    PENDIENTE,
    ASIGNADO,
    EN_CAMINO,
    ENTREGADO,
    FALLIDO,
    CANCELADO
}
