package utnfc.isi.back.logistica.despacho.dto.internal;

/**
 * Datos de un centro que pedidos-service necesita al dar de alta un pedido:
 * que exista, que esté activo y (opcionalmente) que el destino esté dentro de la cobertura,
 * usando Coordenada.estaDentroDelRadio con el mismo criterio que usa despacho.
 */
public record CentroInternoDTO(
        Long id,
        String nombre,
        double latitud,
        double longitud,
        double radioCoberturaKm,
        boolean activo
) {
}
