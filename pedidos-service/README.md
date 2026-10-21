# pedidos-service

Microservicio de **gestión del pedido** (puerto 8081, base `pedidos_db`). Es el dueño del estado de cada pedido: nadie más lo cambia directamente, y despacho-service solo le informa lo que pasó.

## Qué hace

- Perfil de cliente (el comercio), vinculado a su usuario de Keycloak.
- Alta y validación de pedidos: perfil activo, ventana horaria válida, y centro existente y activo (le pregunta a despacho-service).
- Ciclo de vida con transiciones controladas, control de intentos (máximo 2) e historial de cambios.
- Cancelación (solo antes de salir a la calle) y reprogramación de pedidos NO_ASIGNABLE.
- API interna para que despacho-service lea la cola de pendientes e informe asignaciones y entregas.

## Estructura

```
src/main/java/utnfc/isi/back/logistica/pedidos/
├── PedidosApplication.java
├── model/        entidades JPA y reglas de negocio
├── repository/   Spring Data JPA
├── service/      casos de uso y excepciones propias
├── controller/   API REST pública e interna
├── dto/          records de entrada y salida
├── client/       cliente HTTP hacia despacho-service
└── config/       seguridad, RestClient, OpenAPI y manejo de errores
```

### model: las reglas de negocio viven acá

| Clase | Qué hace |
|---|---|
| `Pedido` | Entidad central. Tiene la **máquina de estados** (`TRANSICIONES`) y el único punto que cambia el estado (`cambiarEstado`, que además registra el historial). Reglas: `cancelar`, `reprogramar`, `registrarFallo` (suma el intento y pasa por FALLIDO hacia RECIBIDO o DEVUELTO), `perteneceA` y `ventanaValida`. |
| `EstadoPedido` | RECIBIDO, ASIGNADO, NO_ASIGNABLE, EN_CAMINO, ENTREGADO, FALLIDO, DEVUELTO, CANCELADO. |
| `PrioridadPedido` | NORMAL, URGENTE. |
| `Destinatario` | Embeddable con contacto, dirección y coordenadas; expone su `Coordenada` de commons. |
| `CambioEstado` | Registro inmutable del historial: estado anterior y nuevo, motivo, autor y fecha. |
| `Cliente` | Comercio vinculado a un `keycloakUserId`; si está desactivado no puede crear pedidos. |

### service

| Clase | Qué hace |
|---|---|
| `PedidoService` | Casos de uso del pedido: crear (valida el centro contra despacho), listar con filtros, obtener, historial, cancelar (si está ASIGNADO, primero pide a despacho que lo quite de la ruta) y reprogramar. También atiende los casos de uso internos: pendientes, confirmar asignación en lote, salida y resultado de entrega, todos idempotentes. |
| `ClienteService` | Alta (un perfil por usuario y CUIT único), consulta, actualización, listado y desactivación. |
| `*Exception` | `RecursoNoEncontrado` 404, `AccesoDenegado` 403, `Conflicto` 409, `CentroInexistente` 422 y `ServicioNoDisponible` 503. |

### controller

| Clase | Rutas | Roles |
|---|---|---|
| `ClienteController` | `/api/clientes/me` (POST, GET, PUT), `GET /api/clientes`, `POST /api/clientes/{id}/desactivacion` | CLIENTE para su perfil; DESPACHANTE/ADMIN listan; ADMIN desactiva |
| `PedidoController` | `POST/GET /api/pedidos`, `GET /{id}`, `GET /{id}/cambios`, `POST /{id}/cancelacion`, `POST /{id}/reprogramacion` | El CLIENTE solo ve y opera lo propio; DESPACHANTE y ADMIN ven todo; reprogramar es solo DESPACHANTE/ADMIN |
| `PedidoInternoController` | `GET /internal/pedidos/pendientes`, `POST /asignaciones`, `POST /{id}/en-camino`, `POST /{id}/resultado-entrega` | Solo SERVICE (y el gateway no lo expone) |

### Resto

- `dto/`: requests con Bean Validation y responses con método `from(entidad)`. `ConfirmacionAsignacionRequest/Response` son el contrato con despacho para confirmar una planificación.
- `client/DespachoClient`: `obtenerCentro` y `quitarDeRuta`. Traduce los errores HTTP a excepciones propias (404 a centro inexistente, 409 a "ya salió a la calle", timeout o 5xx a 503) para no exponer detalles de despacho.
- `config/SecurityConfig`: resource server JWT; `/internal/**` solo SERVICE; roles finos con `@PreAuthorize`.
- `config/RestClientConfig`: RestClient hacia despacho con timeouts (2 s de conexión, 3 s de lectura) y token client credentials, que Spring cachea.
- `config/PedidosExceptionHandler`: mapea las excepciones propias; el resto lo cubre el handler de commons.

## Tests

`mvn test`, con 43 tests:

| Clase | Qué prueba |
|---|---|
| `PedidoTest` | Máquina de estados, cancelación, fallos e intentos, sin Spring |
| `PedidoServiceTest` | Casos de uso con Mockito: centro inexistente o inactivo, despacho caído, acceso ajeno, confirmación en lote, cancelación coordinada, reprogramación e idempotencia |
| `ClienteServiceTest` | Alta, perfil duplicado, CUIT duplicado, perfil inexistente y desactivación |
| `PedidoControllerSecurityTest` | Acceso por rol, incluido `/internal` |

## Estado

Completo para el alcance del TPI.
