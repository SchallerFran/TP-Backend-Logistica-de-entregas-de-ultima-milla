package utnfc.isi.back.logistica.despacho.dto.internal;

/** Prioridad del pedido tal como la envía pedidos-service (los URGENTE se planifican primero). */
public enum Prioridad {
    NORMAL,
    URGENTE
}
