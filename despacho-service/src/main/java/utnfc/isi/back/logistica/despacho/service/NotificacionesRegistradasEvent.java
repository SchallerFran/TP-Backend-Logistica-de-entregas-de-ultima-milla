package utnfc.isi.back.logistica.despacho.service;

/** Se publica cuando una transacción dejó notificaciones nuevas en el outbox. */
public record NotificacionesRegistradasEvent(int cantidad) {
}
