package utnfc.isi.back.logistica.pedidos.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import utnfc.isi.back.logistica.commons.Coordenada;

/**
 * Datos de entrega del pedido. Las columnas se prefijan con "destinatario_"
 * desde los @AttributeOverrides de Pedido.
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Destinatario {

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(length = 30)
    private String telefono;

    @Column(nullable = false, length = 200)
    private String direccion;

    @Column(nullable = false)
    private Double latitud;

    @Column(nullable = false)
    private Double longitud;

    @Column(length = 255)
    private String referencias;

    /** Coordenada del JAR comun (valida rangos y calcula distancia Haversine). */
    public Coordenada coordenada() {
        return new Coordenada(latitud, longitud);
    }
}
