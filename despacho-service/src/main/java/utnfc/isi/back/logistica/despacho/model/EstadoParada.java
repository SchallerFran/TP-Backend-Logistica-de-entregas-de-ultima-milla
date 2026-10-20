package utnfc.isi.back.logistica.despacho.model;

/** Resultado de cada parada. */
public enum EstadoParada {
    PENDIENTE,
    ENTREGADA,
    /** No se pudo entregar; requiere motivoFallo. */
    FALLIDA
}
