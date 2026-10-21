# api-gateway

Punto de entrada único al backend (puerto 8080), con Spring Cloud Gateway (WebFlux). Los consumidores solo conocen esta URL: no necesitan saber en qué puerto ni en qué servicio vive cada funcionalidad.

## Estructura

```
src/main/java/utnfc/isi/back/logistica/gateway/
├── GatewayApplication.java
└── SecurityConfig.java       Security Filter
src/main/resources/application.yml   rutas y validación de JWT
```

## Rutas

| Prefijo | Destino |
|---|---|
| `/api/pedidos/**` | pedidos-service (`PEDIDOS_SERVICE_URL`, por defecto `http://localhost:8081`) |
| `/api/clientes/**` | pedidos-service |
| `/api/despacho/**` | despacho-service (`DESPACHO_SERVICE_URL`, por defecto `http://localhost:8082`) |

## Security Filter

`SecurityConfig` es la primera barrera:

- **Valida el JWT** de Keycloak (firma con las claves de `jwk-set-uri` y emisor `issuer-uri`). Sin un token válido, responde 401.
- **Bloquea `/internal/**`** (`denyAll`): las APIs entre servicios no existen desde afuera.
- Deja pasar sin token solo `/actuator/health`.

La autorización por rol no se repite acá: la hace cada microservicio, que conoce sus reglas (incluidas las que dependen de datos, como "el cliente solo ve lo suyo"). Así el gateway no hay que tocarlo cada vez que cambia un permiso, y cada servicio queda protegido aunque alguien lo llame sin pasar por el gateway.

## Estado

Completo. No usa `logistica-commons` porque es reactivo y el JAR común asume Spring MVC.
