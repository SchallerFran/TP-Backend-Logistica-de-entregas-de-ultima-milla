package utnfc.isi.back.logistica.despacho.model;

public enum EstadoNotificacion {
    /** Todavía no fue aceptada por pedidos-service (nunca se envió o falló por red). */
    PENDIENTE,
    /** pedidos-service la aplicó. */
    ENVIADA,
    /** pedidos-service la rechazó por una regla de negocio (4xx) o se agotaron los intentos: requiere revisión. */
    ERROR
}
