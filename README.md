# TPI Backend - Logistica de entregas de ultima milla

Sistema de microservicios para gestion de pedidos y despacho de entregas de
ultima milla.

- **Java 21**, **Spring Boot 3.5.16**, **Maven**, **PostgreSQL 17**, **Keycloak 26**, **Docker Compose**
- Package base / groupId: `utnfc.isi.back.logistica`

## Proyectos

Son **cuatro proyectos Maven independientes**: no hay POM padre ni reactor.
Cada uno tiene su propio `pom.xml` y se construye por separado.

| Proyecto | Artifact | Puerto | Descripcion |
|---|---|---|---|
| `logistica-commons` | `logistica-commons:1.0.0` | - | JAR comun (errores, roles, enums, Coordenada) |
| `api-gateway` | `api-gateway:1.0.0` | 8080 | Spring Cloud Gateway (WebFlux) + validacion de JWT |
| `pedidos-service` | `pedidos-service:1.0.0` | 8081 | Pedidos (Spring MVC + JPA) |
| `despacho-service` | `despacho-service:1.0.0` | 8082 | Despacho/envios + Google Distance Matrix |

`logistica-commons` se consume como dependencia normal por
`groupId:artifactId:version`, asi que **hay que instalarlo en el repositorio
local (`~/.m2`) antes de compilar los servicios**.

El gateway **no** usa `logistica-commons`: es reactivo (WebFlux) y el JAR comun
asume Spring MVC.

## Orden de build (local)

```bash
# 1. Primero el JAR comun -> queda en ~/.m2
cd logistica-commons && mvn clean install

# 2. Despues, en cualquier orden
cd ../pedidos-service  && mvn clean package
cd ../despacho-service && mvn clean package
cd ../api-gateway      && mvn clean package
```

Si se saltea el paso 1, los servicios fallan con
`Could not resolve dependencies ... logistica-commons:jar:1.0.0`.

## Levantar todo con Docker

```bash
cp .env.example .env     # y completar GOOGLE_MAPS_API_KEY
docker compose up --build
```

No hace falta compilar nada a mano: los `Dockerfile` de `pedidos-service` y
`despacho-service` son multi-stage, se construyen con contexto en la raiz del
repo y hacen `mvn clean install` de `logistica-commons` dentro de la imagen de
build antes de empaquetar el servicio.

Puertos publicados al host (lo demas queda en la red interna de Docker):

| Servicio | URL |
|---|---|
| API Gateway | http://localhost:8080 |
| Keycloak | http://localhost:8180 (admin/admin por defecto) |

Swagger UI de cada servicio (solo accesible desde dentro de la red de Docker o
levantando el servicio localmente): `http://localhost:8081/swagger-ui.html` y
`http://localhost:8082/swagger-ui.html`.

## Seguridad

Keycloak importa el realm `logistica` desde `infra/realm-logistica.json`:

- Roles: `CLIENTE`, `DESPACHANTE`, `REPARTIDOR`, `ADMIN`, `SERVICE`
- Client publico `logistica-public` (password grant, para pruebas)
- Client confidencial `logistica-service` (service account con rol `SERVICE`,
  para llamadas entre microservicios)
- Un usuario de prueba por rol: `cliente1`, `despachante1`, `repartidor1`,
  `admin1` (la contrasena es igual al usuario)

Obtener un token:

```bash
curl -s -X POST \
  http://localhost:8180/realms/logistica/protocol/openid-connect/token \
  -d client_id=logistica-public \
  -d grant_type=password \
  -d username=cliente1 \
  -d password=cliente1
```

Usarlo contra el gateway:

```bash
curl http://localhost:8080/api/pedidos/ping -H "Authorization: Bearer <token>"
```

El gateway valida el JWT y exige autenticacion; la autorizacion por rol la hace
cada microservicio con `KeycloakRoleConverter` (mapea `realm_access.roles` a
authorities `ROLE_*`). Los paths `/internal/**` estan bloqueados en el gateway y
restringidos al rol `SERVICE` en cada servicio.

### Issuer de Keycloak en Docker

El token se pide desde el host (`http://localhost:8180`) pero se valida dentro
de la red de Docker, donde ese host no existe. La solucion aplicada:

- `KC_HOSTNAME=http://localhost:8180` en el contenedor de Keycloak, para que el
  claim `iss` sea siempre `http://localhost:8180/realms/logistica`.
- En los servicios, `issuer-uri` apunta a ese host publico (es solo el valor
  esperado del claim) y `jwk-set-uri` a la URL interna
  `http://keycloak:8080/realms/logistica/protocol/openid-connect/certs`.

Cuando `jwk-set-uri` esta definido, Spring Security no hace el discovery OIDC
contra el issuer, por lo que la validacion funciona sin necesidad de que el
contenedor resuelva `localhost:8180`.

## Variables de entorno

Todas las credenciales y la API key de Google salen de `.env` (ver
`.env.example`). El `.env` no se versiona.

Las contrasenas de las bases estan tambien en `infra/init-db.sql` porque un
script `.sql` no puede leer variables de entorno: si se cambian, hay que
cambiarlas en los dos lados.

## Estado actual

Esqueleto: estructura, configuracion y una clase minima de ejemplo por capa.
Sin logica de negocio.
