package utnfc.isi.back.logistica.despacho.model;

/** Ciclo de vida de la ruta. */
public enum EstadoRuta {
    /** Ruta armada, esperando que pedidos-service confirme los pedidos. */
    BORRADOR,
    /** Todos sus pedidos fueron confirmados como ASIGNADO. */
    CONFIRMADA,
    /** El repartidor inició la ruta. */
    EN_CURSO,
    /** Todas las paradas tienen resultado. */
    FINALIZADA
}
