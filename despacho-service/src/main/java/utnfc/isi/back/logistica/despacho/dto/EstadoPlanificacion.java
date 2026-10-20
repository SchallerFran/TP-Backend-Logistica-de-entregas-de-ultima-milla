package utnfc.isi.back.logistica.despacho.dto;

public enum EstadoPlanificacion {
    /** pedidos-service confirmó: las rutas quedaron CONFIRMADA. */
    CONFIRMADA,
    /** pedidos-service no respondió: las rutas quedaron en BORRADOR, se puede reintentar la confirmación. */
    PENDIENTE_CONFIRMACION,
    /** No se armó ninguna ruta (no había pedidos o ninguno era asignable). */
    SIN_RUTAS
}
