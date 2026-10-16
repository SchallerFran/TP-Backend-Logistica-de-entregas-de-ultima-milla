package utnfc.isi.back.logistica.despacho.service.planificacion;

/**
 * Motivo concreto por el que un pedido no entra en ninguna ruta. Se informa a pedidos-service
 * (queda en el historial del pedido) y al despachante, para que pueda intervenir.
 */
public enum MotivoNoAsignable {
    VENTANA_FUERA_DE_FECHA("La ventana horaria no corresponde a la fecha planificada"),
    FUERA_DE_COBERTURA("El destino está fuera del radio de cobertura del centro"),
    SIN_VEHICULO_REFRIGERADO("Requiere frío y no hay vehículos refrigerados disponibles"),
    EXCEDE_CAPACIDAD_VEHICULOS("El peso o volumen supera la capacidad de todos los vehículos disponibles"),
    SIN_RECURSOS_DISPONIBLES("No quedan vehículos y repartidores libres para la fecha"),
    CAPACIDAD_AGOTADA("No queda capacidad de carga en las rutas posibles"),
    EXCEDE_AUTONOMIA("Ninguna ruta posible respeta la autonomía del vehículo"),
    EXCEDE_TURNO("Ninguna ruta posible termina dentro del turno del repartidor"),
    VENTANA_HORARIA_INALCANZABLE("No se puede llegar dentro de la ventana horaria con ninguna ruta posible");

    private final String descripcion;

    MotivoNoAsignable(String descripcion) {
        this.descripcion = descripcion;
    }

    public String descripcion() {
        return descripcion;
    }
}
