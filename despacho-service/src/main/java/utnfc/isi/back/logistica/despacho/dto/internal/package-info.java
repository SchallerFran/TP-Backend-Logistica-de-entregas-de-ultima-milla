/**
 * Contrato de la API interna (/internal/**) entre pedidos-service y despacho-service.
 * <p>
 * Viven en el JAR común porque son el "lenguaje publicado" entre los dos servicios: si cambia un
 * campo, cambia para ambos en el mismo commit y el compilador avisa en los dos lados.
 * Son records inmutables, sin anotaciones de JPA ni de Jackson (Jackson 2.12+ serializa records).
 * <p>
 * Endpoints que los usan (ver docs/CONTRATO-INTERNO.md):
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
