-- Datos semilla de despacho-service (idempotente: se puede ejecutar en cada arranque).
-- No se fijan ids (las columnas son IDENTITY): se usan las claves naturales únicas para ON CONFLICT.
-- IMPORTANTE: los keycloak_user_id de los repartidores deben coincidir con el "id" de esos usuarios
-- en infra/realm-logistica.json (ver README).

INSERT INTO centro_distribucion (nombre, direccion, latitud, longitud, radio_cobertura_km, activo)
VALUES ('Centro Córdoba', 'Av. Colón 750, Córdoba', -31.4135, -64.1810, 25, true)
ON CONFLICT (nombre) DO NOTHING;

INSERT INTO centro_distribucion (nombre, direccion, latitud, longitud, radio_cobertura_km, activo)
VALUES ('Centro Neuquén', 'Av. Argentina 200, Neuquén', -38.9516, -68.0591, 20, true)
ON CONFLICT (nombre) DO NOTHING;

-- Flota del centro Córdoba: dos utilitarios comunes y uno refrigerado (más caro por km)
INSERT INTO vehiculo (patente, peso_max_kg, vol_max_m3, autonomia_km, costo_por_km, tiene_frio, disponible, centro_id)
SELECT 'AB123CD', 800, 6, 250, 450, false, true, c.id FROM centro_distribucion c WHERE c.nombre = 'Centro Córdoba'
ON CONFLICT (patente) DO NOTHING;

INSERT INTO vehiculo (patente, peso_max_kg, vol_max_m3, autonomia_km, costo_por_km, tiene_frio, disponible, centro_id)
SELECT 'AC456EF', 1500, 12, 300, 600, false, true, c.id FROM centro_distribucion c WHERE c.nombre = 'Centro Córdoba'
ON CONFLICT (patente) DO NOTHING;

INSERT INTO vehiculo (patente, peso_max_kg, vol_max_m3, autonomia_km, costo_por_km, tiene_frio, disponible, centro_id)
SELECT 'AD789GH', 1000, 8, 200, 750, true, true, c.id FROM centro_distribucion c WHERE c.nombre = 'Centro Córdoba'
ON CONFLICT (patente) DO NOTHING;

INSERT INTO vehiculo (patente, peso_max_kg, vol_max_m3, autonomia_km, costo_por_km, tiene_frio, disponible, centro_id)
SELECT 'AE012IJ', 800, 6, 250, 480, false, true, c.id FROM centro_distribucion c WHERE c.nombre = 'Centro Neuquén'
ON CONFLICT (patente) DO NOTHING;

-- Repartidores (ids de usuario fijos en el realm de Keycloak)
INSERT INTO repartidor (nombre, keycloak_user_id, turno_inicio, turno_fin, activo, centro_id)
SELECT 'Juan Pérez', 'b1e7c7a0-0000-4000-8000-000000000001', '08:00', '16:00', true, c.id
FROM centro_distribucion c WHERE c.nombre = 'Centro Córdoba'
ON CONFLICT (keycloak_user_id) DO NOTHING;

INSERT INTO repartidor (nombre, keycloak_user_id, turno_inicio, turno_fin, activo, centro_id)
SELECT 'María Gómez', 'b1e7c7a0-0000-4000-8000-000000000002', '12:00', '20:00', true, c.id
FROM centro_distribucion c WHERE c.nombre = 'Centro Córdoba'
ON CONFLICT (keycloak_user_id) DO NOTHING;

INSERT INTO repartidor (nombre, keycloak_user_id, turno_inicio, turno_fin, activo, centro_id)
SELECT 'Lucas Sosa', 'b1e7c7a0-0000-4000-8000-000000000003', '08:00', '16:00', true, c.id
FROM centro_distribucion c WHERE c.nombre = 'Centro Neuquén'
ON CONFLICT (keycloak_user_id) DO NOTHING;
