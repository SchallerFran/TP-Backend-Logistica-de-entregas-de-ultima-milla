/**
 * Contrato de la API interna (/internal/**) entre pedidos-service y despacho-service.
 * <p>
 * Cada servicio tiene su propia copia del contrato (no viven en el JAR común): así ninguno depende de
 * las clases internas del otro y los enums del pedido siguen siendo de pedidos-service.
 * Son records inmutables, sin anotaciones de JPA ni de Jackson (Jackson 2.12+ serializa records).
 * <p>
 * Endpoints que los usan:
 * <ul>
 *   <li>pedidos-service  GET  /internal/pedidos/pendientes?centroId&amp;fecha → List&lt;PedidoPendienteDTO&gt;</li>
 *   <li>pedidos-service  POST /internal/pedidos/asignaciones → ConfirmacionAsignacionResponse</li>
 *   <li>pedidos-service  POST /internal/pedidos/{id}/en-camino (InicioEntregaRequest)</li>
 *   <li>pedidos-service  POST /internal/pedidos/{id}/resultado-entrega (ResultadoEntregaRequest)</li>
 *   <li>despacho-service GET  /internal/centros/{id} → CentroInternoDTO</li>
 *   <li>despacho-service DELETE /internal/paradas/pedido/{pedidoId}</li>
 * </ul>
 */
package utnfc.isi.back.logistica.despacho.dto.internal;
