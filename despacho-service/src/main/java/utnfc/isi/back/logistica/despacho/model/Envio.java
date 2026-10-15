package utnfc.isi.back.logistica.despacho.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import utnfc.isi.back.logistica.commons.EstadoPedido;

/**
 * Entidad minima de ejemplo: solo fija la convencion, sin logica de negocio.
 */
@Entity
@Table(name = "envios")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Envio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "pedido_id", nullable = false)
    private Long pedidoId;

    @Column(name = "repartidor_id")
    private String repartidorId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoPedido estado;
}
