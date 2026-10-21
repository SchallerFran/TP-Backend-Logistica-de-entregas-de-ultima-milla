# despacho-service

Microservicio de **operación logística** (puerto 8082, base `despacho_db`). Es el dueño de los recursos (centros, vehículos, repartidores) y de las rutas. Del pedido solo conoce lo que necesita para planificar: coordenadas, peso, volumen, ventana, prioridad y frío. No guarda datos personales.

## Qué hace

- ABM de datos maestros: centros, vehículos y repartidores (las bajas son lógicas).
- **Planificación**: arma las rutas del día para un centro respetando todas las restricciones, y confirma con pedidos-service.
- **Ejecución**: el repartidor ve su ruta, la inicia y registra cada parada; pedidos-service se entera por un outbox con reintentos.
- **Distancias** reales con Google Maps Distance Matrix, con caché y estimación de respaldo.
- Supervisión del despachante: rutas por fecha y estado, y notificaciones pendientes o con error.

## Estructura

```
src/main/java/utnfc/isi/back/logistica/despacho/
├── DespachoServiceApplication.java   habilita scheduling (outbox) y async
├── model/            entidades JPA y enums de estado
├── repository/       Spring Data JPA
├── service/          casos de uso
│   └── planificacion/  algoritmo de asignación en Java puro (sin Spring ni base)
├── controller/       API REST pública
│   └── internal/       API para pedidos-service
├── dto/              requests/responses y mapper
│   └── internal/       contrato con pedidos-service
├── client/           Google Maps y pedidos-service
├── exception/        excepciones y handler (ProblemDetail con "codigo")
├── security/Roles    nombres de los roles
└── config/           seguridad, RestClients, propiedades, reloj y OpenAPI
src/main/resources/data.sql   datos semilla: 2 centros, 4 vehículos, 3 repartidores
```

### model

| Clase | Qué representa |
|---|---|
| `CentroDistribucion` | Centro con coordenadas y radio de cobertura en km |
| `Vehiculo` | Capacidad de peso y volumen, autonomía, costo por km, refrigeración y disponibilidad; pertenece a un centro |
| `Repartidor` | Turno (inicio y fin) y `keycloakUserId`, que debe coincidir con el usuario del realm |
| `Ruta` | Un día, un vehículo y un repartidor, con paradas ordenadas, km, duración, costo y la marca `estimada`. Estados: BORRADOR → CONFIRMADA → EN_CURSO → FINALIZADA |
| `Parada` | Copia de los datos del pedido al planificar, con ETA, estado (PENDIENTE / ENTREGADA / FALLIDA) y motivo de fallo |
| `NotificacionPedido` | **Outbox**: un aviso pendiente para pedidos-service (EN_CAMINO o RESULTADO_ENTREGA), con estado PENDIENTE / ENVIADA / ERROR |
| `DistanciaCache` | Distancias ya consultadas a Google, con fecha de consulta |

### service

| Clase | Qué hace |
|---|---|
| `PlanificacionService` | Orquesta la planificación (la saga): descarta los borradores previos, carga los recursos libres, pide los pendientes a pedidos, calcula la matriz de distancias, planifica, persiste en BORRADOR y confirma con pedidos. Si pedidos no responde, las rutas quedan en BORRADOR y se puede reintentar solo la confirmación. |
| `planificacion/PlanificadorRutas` | Heurística de **inserción más barata**: primero los URGENTE y después los de ventana más ajustada; prueba cada posición de cada ruta abierta o abre una ruta nueva; elige la de menor costo (km adicionales × costo por km). Si un pedido no entra en ninguna, informa el motivo (`MotivoNoAsignable`). |
| `planificacion/CalculadoraRuta` | Simula una ruta y verifica todas las restricciones a la vez: capacidad, frío, ventanas, turno y autonomía. |
| `planificacion/*` (records) | Datos del algoritmo: `MatrizDistancias`, `ParametrosPlan`, `VehiculoPlan`, `RepartidorPlan`, `RutaPlan`, `ParadaPlan`, `EvaluacionRuta`, `NoAsignado`, `ResultadoPlanificacion`, `FuenteDistancia`. |
| `DistanciaService` | Resuelve cada par de puntos en este orden: caché vigente, Google (en bloques de 10×10), caché vencida y estimación Haversine × factor de rodeo. |
| `EjecucionRutaService` | La ruta del repartidor, iniciar y registrar el resultado. Guarda la notificación en la misma transacción (outbox); también ofrece las búsquedas del despachante. |
| `NotificacionPedidoService` | Envía el outbox a pedidos-service apenas se confirma la transacción y cada 15 s. Si pedidos no responde, corta la vuelta y reintenta después; si pedidos rechaza un aviso, lo marca ERROR para revisión. |
| `CancelacionService` | Lo invoca pedidos antes de cancelar: quita la parada y recalcula la ruta, o responde 409 si el pedido ya está en la calle. |
| `RutaRecalculador` | Quita paradas de una ruta ya guardada y recalcula km, costo y ETAs. |
| `CentroService`, `VehiculoService`, `RepartidorService` | ABM de datos maestros. |

### controller

| Clase | Rutas | Roles |
|---|---|---|
| `CentroController`, `VehiculoController`, `RepartidorController` | `/api/despacho/{centros\|vehiculos\|repartidores}` (GET, POST, PUT, DELETE) | Lectura: ADMIN/DESPACHANTE; escritura: ADMIN |
| `PlanificacionController` | `POST /api/despacho/planificaciones`, `POST /planificaciones/confirmacion` | DESPACHANTE |
| `RutaController` | `GET /api/despacho/rutas`, `GET /{id}`, `DELETE /{id}` (descarta un BORRADOR) | DESPACHANTE (ADMIN puede leer) |
| `MiRutaController` | `GET /api/despacho/mi-ruta`, `POST /{rutaId}/inicio`, `POST /{rutaId}/paradas/{paradaId}/resultado` | REPARTIDOR, y solo sobre su propia ruta |
| `NotificacionController` | `GET /api/despacho/notificaciones`, `POST /{id}/reintento` | DESPACHANTE/ADMIN |
| `DistanciaController` | `GET /api/despacho/distancias?origen&destino` | DESPACHANTE/ADMIN |
| `internal/InternalDespachoController` | `GET /internal/centros/{id}`, `DELETE /internal/paradas/pedido/{id}` | Solo SERVICE |

### Resto

- `client/GoogleDistanceMatrixClient`: llama a Distance Matrix y traduce sus fallos a `GoogleNoDisponibleException`.
- `client/PedidosClient`: llama a la API interna de pedidos. Distingue el fallo **transitorio** (`PedidosNoDisponibleException`: red, timeout o 5xx, se reintenta) del **rechazo** (`PedidosRechazoException`: 4xx, no se reintenta).
- `config/DespachoProperties`: parámetros de `application.yml` bajo `despacho.*`: tiempo de servicio por parada, timeouts, TTL de la caché, factor de rodeo, velocidad promedio e intervalo del outbox.
- `config/ClockConfig`: reloj en la zona `America/Argentina/Cordoba`, inyectable para tests.
- `exception/DespachoExceptionHandler`: errores como `ProblemDetail`, con un `codigo` estable (por ejemplo `PEDIDO_EN_CAMINO`); toma prioridad sobre el handler de commons.

## Tests

`mvn test`, con 11 tests en `PlanificadorRutasTest`:

- agrupar pedidos cercanos con el vehículo más barato;
- urgentes primero y abrir otra ruta cuando la primera se llena;
- frío y no gastar el vehículo refrigerado sin necesidad;
- cobertura;
- ventanas (inalcanzable, espera y orden de paradas);
- turno del repartidor;
- falta de recursos libres.

## Estado

Completo para el alcance del TPI. Sin `GOOGLE_MAPS_API_KEY` funciona con caché y estimación.
