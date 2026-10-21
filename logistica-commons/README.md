# logistica-commons

JAR Maven (`utnfc.isi.back.logistica:logistica-commons:1.2.0`) que usan `pedidos-service` y `despacho-service` como dependencia normal. No es un ejecutable: es una librería.

## Para qué está

Reúne lo que tiene que ser **idéntico** en los dos microservicios, para no copiar código entre proyectos:

- el mismo formato de error;
- la misma lectura de roles del token de Keycloak;
- el mismo cálculo geográfico.

Lo que es propio del dominio de cada servicio (entidades, enums de estado, DTOs) queda dentro de cada uno, a propósito: así ninguno depende de las clases internas del otro.

## Estructura

```
src/main/java/utnfc/isi/back/logistica/commons/
├── ApiError.java               cuerpo JSON uniforme de error
├── GlobalExceptionHandler.java @RestControllerAdvice con los mapeos comunes de excepciones
├── KeycloakRoleConverter.java  realm_access.roles del JWT → authorities ROLE_*
└── Coordenada.java             lat/lng validada, Haversine, cobertura y redondeo
src/test/java/.../CoordenadaTest.java
```

| Clase | Qué hace |
|---|---|
| `ApiError` | Record `timestamp, status, error, mensaje, path`, que se devuelve en todos los errores. |
| `GlobalExceptionHandler` | Traduce excepciones a HTTP: validación 400, `IllegalArgumentException` 422 (regla de negocio), `RestClientException` 503, acceso denegado 403, ruta inexistente 404 y cualquier otra 500. Cada servicio puede agregar su propio handler con prioridad. |
| `KeycloakRoleConverter` | Lo usa Spring Security en cada servicio para que `hasRole('CLIENTE')` funcione con los roles de realm de Keycloak. |
| `Coordenada` | Valida rangos y calcula la distancia en línea recta (Haversine). También verifica si un punto está dentro del radio de un centro, redondea a 4 decimales (la clave de la caché de distancias) y convierte desde y hacia el formato `"lat,lng"` de Google. |

Las dependencias de Spring están en scope `provided`: el JAR no impone versiones, cada servicio usa las de su Spring Boot. Los servicios registran las clases agregando `utnfc.isi.back.logistica.commons` a `scanBasePackages`.

## Uso

```bash
mvn clean install     # compila, corre CoordenadaTest e instala en ~/.m2
```

Hay que repetirlo cada vez que se modifica el JAR y, si cambia la API, subir la versión en este `pom.xml` y en el de los dos servicios.

## Estado

Completo. El gateway no lo usa porque es reactivo (WebFlux) y este JAR asume Spring MVC.
