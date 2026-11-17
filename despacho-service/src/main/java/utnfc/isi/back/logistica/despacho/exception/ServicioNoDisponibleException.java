package utnfc.isi.back.logistica.despacho.exception;

public class ServicioNoDisponibleException extends RuntimeException {

    private final String servicio;

    public ServicioNoDisponibleException(String servicio, String mensaje, Throwable causa) {
        super(mensaje, causa);
        this.servicio = servicio;
    }

    public String getServicio() {
        return servicio;
    }
}
