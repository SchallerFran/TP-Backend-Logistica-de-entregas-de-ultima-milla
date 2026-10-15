package utnfc.isi.back.logistica.pedidos.model;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "pedido")
@Getter
@Setter
@NoArgsConstructor
public class Pedido {

    /** Autor de los cambios de estado automaticos. */
    public static final String USUARIO_SISTEMA = "SISTEMA";

    /** Cantidad de intentos fallidos a partir de la cual el pedido se devuelve. */
    private static final int MAX_INTENTOS = 2;

    /**
     * Transiciones permitidas. ENTREGADO conserva una unica salida (DEVUELTO)
     * para casos particulares; DEVUELTO y CANCELADO son finales.
     */
    private static final Map<EstadoPedido, Set<EstadoPedido>> TRANSICIONES = Map.of(
            EstadoPedido.RECIBIDO, Set.of(EstadoPedido.ASIGNADO, EstadoPedido.CANCELADO),
            EstadoPedido.ASIGNADO, Set.of(EstadoPedido.EN_CAMINO, EstadoPedido.CANCELADO, EstadoPedido.REPROGRAMADO),
            EstadoPedido.EN_CAMINO, Set.of(EstadoPedido.ENTREGADO, EstadoPedido.REPROGRAMADO, EstadoPedido.FALLIDO),
            EstadoPedido.REPROGRAMADO, Set.of(EstadoPedido.EN_CAMINO, EstadoPedido.CANCELADO),
            EstadoPedido.FALLIDO, Set.of(EstadoPedido.RECIBIDO, EstadoPedido.DEVUELTO),
            EstadoPedido.ENTREGADO, Set.of(EstadoPedido.DEVUELTO),
            EstadoPedido.DEVUELTO, Set.of(),
            EstadoPedido.CANCELADO, Set.of());

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "nombre", column = @Column(name = "destinatario_nombre", nullable = false, length = 120)),
            @AttributeOverride(name = "telefono", column = @Column(name = "destinatario_telefono", length = 30)),
            @AttributeOverride(name = "direccion", column = @Column(name = "destinatario_direccion", nullable = false, length = 200)),
            @AttributeOverride(name = "latitud", column = @Column(name = "destinatario_latitud", nullable = false)),
            @AttributeOverride(name = "longitud", column = @Column(name = "destinatario_longitud", nullable = false)),
            @AttributeOverride(name = "referencias", column = @Column(name = "destinatario_referencias", length = 255))
    })
    private Destinatario destinatario;

    /** Referencia logica al centro de despacho-service (sin relacion JPA). */
    @NotNull
    @Column(name = "centro_id", nullable = false)
    private Long centroId;

    @NotNull
    @Positive
    @Column(name = "peso_kg", nullable = false)
    private Double pesoKg;

    @NotNull
    @Positive
    @Column(name = "volumen_m3", nullable = false)
    private Double volumenM3;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PrioridadPedido prioridad = PrioridadPedido.NORMAL;

    @Column(name = "ventana_desde", nullable = false)
    private LocalDateTime ventanaDesde;

    @Column(name = "ventana_hasta", nullable = false)
    private LocalDateTime ventanaHasta;

    @Column(name = "requiere_frio", nullable = false)
    private boolean requiereFrio = false;

    /** Solo se modifica desde cambiarEstado(). */
    @Enumerated(EnumType.STRING)
    @Setter(AccessLevel.NONE)
    @Column(nullable = false, length = 20)
    private EstadoPedido estado = EstadoPedido.RECIBIDO;

    @Setter(AccessLevel.NONE)
    @Column(nullable = false)
    private int intentos = 0;

    @Setter(AccessLevel.NONE)
    @Column(name = "creado_en", nullable = false, updatable = false)
    private LocalDateTime creadoEn;

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("fecha ASC")
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private List<CambioEstado> cambios = new ArrayList<>();

    @PrePersist
    void alPersistir() {
        this.creadoEn = LocalDateTime.now();
        if (cambios.isEmpty()) {
            cambios.add(new CambioEstado(this, null, EstadoPedido.RECIBIDO, "Alta del pedido", USUARIO_SISTEMA));
        }
    }

    /** Historial de cambios: vista no modificable. */
    public List<CambioEstado> getCambios() {
        return Collections.unmodifiableList(cambios);
    }

    /**
     * Unico punto de modificacion del estado.
     *
     * @throws IllegalArgumentException si la transicion no esta permitida
     *         (el GlobalExceptionHandler del JAR comun la mapea a 422)
     */
    public void cambiarEstado(EstadoPedido nuevo, String motivo, String usuario) {
        if (nuevo == null) {
            throw new IllegalArgumentException("El nuevo estado es obligatorio");
        }
        if (usuario == null || usuario.isBlank()) {
            throw new IllegalArgumentException("El usuario que realiza el cambio es obligatorio");
        }
        if (!TRANSICIONES.getOrDefault(estado, Set.of()).contains(nuevo)) {
            throw new IllegalArgumentException("Transicion no permitida: " + estado + " -> " + nuevo);
        }
        EstadoPedido anterior = this.estado;
        this.estado = nuevo;
        this.cambios.add(new CambioEstado(this, anterior, nuevo, motivo, usuario));
    }

    public boolean puedeCancelarse() {
        return estado == EstadoPedido.RECIBIDO || estado == EstadoPedido.ASIGNADO;
    }

    public void cancelar(String motivo, String usuario) {
        if (!puedeCancelarse()) {
            throw new IllegalArgumentException("El pedido no puede cancelarse en estado " + estado);
        }
        cambiarEstado(EstadoPedido.CANCELADO, motivo, usuario);
    }

    /**
     * Registra un intento de entrega fallido. Solo desde EN_CAMINO.
     * Tras el paso a FALLIDO, el sistema reintenta (vuelve a RECIBIDO) o
     * devuelve el pedido al alcanzar el maximo de intentos.
     */
    public void registrarFallo(String motivo, String usuario) {
        if (estado != EstadoPedido.EN_CAMINO) {
            throw new IllegalArgumentException(
                    "Solo se puede registrar un fallo desde EN_CAMINO, estado actual: " + estado);
        }
        intentos++;
        cambiarEstado(EstadoPedido.FALLIDO, motivo, usuario);
        if (intentos >= MAX_INTENTOS) {
            cambiarEstado(EstadoPedido.DEVUELTO,
                    "Maximo de intentos alcanzado (" + intentos + ")", USUARIO_SISTEMA);
        } else {
            cambiarEstado(EstadoPedido.RECIBIDO,
                    "Reintento automatico (intento " + intentos + ")", USUARIO_SISTEMA);
        }
    }

    public boolean esEstadoFinal() {
        return estado == EstadoPedido.ENTREGADO
                || estado == EstadoPedido.DEVUELTO
                || estado == EstadoPedido.CANCELADO;
    }

    public boolean perteneceA(String keycloakUserId) {
        return cliente != null && Objects.equals(cliente.getKeycloakUserId(), keycloakUserId);
    }

    public boolean ventanaValida() {
        return ventanaDesde != null && ventanaHasta != null && ventanaHasta.isAfter(ventanaDesde);
    }
}
