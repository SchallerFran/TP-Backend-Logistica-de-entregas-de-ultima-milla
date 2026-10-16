/**
 * Núcleo de la planificación. Es Java puro a propósito (sin Spring, sin JPA, sin HTTP):
 * recibe datos ya cargados y una matriz de distancias ya resuelta, y devuelve rutas y pedidos no
 * asignables. Así la regla de negocio más importante del dominio se puede testear y explicar
 * aislada de la infraestructura.
 */
package utnfc.isi.back.logistica.despacho.service.planificacion;
