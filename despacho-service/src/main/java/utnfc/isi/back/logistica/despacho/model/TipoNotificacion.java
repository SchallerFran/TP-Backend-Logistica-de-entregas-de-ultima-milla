package utnfc.isi.back.logistica.despacho.model;

/** Hechos de la ejecución que pedidos-service tiene que reflejar en el estado del pedido. */
public enum TipoNotificacion {
    /** La ruta se inició: ASIGNADO → EN_CAMINO. */
    EN_CAMINO,
    /** El repartidor registró el resultado de la parada. */
    RESULTADO_ENTREGA
}
