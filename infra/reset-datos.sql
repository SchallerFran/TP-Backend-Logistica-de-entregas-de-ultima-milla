-- Vuelve las bases al estado inicial para repetir la demo o la colección de Postman.
-- Borra pedidos, clientes, rutas y avisos; conserva la semilla (centros, vehículos y repartidores)
-- y la caché de distancias. Se ejecuta como superusuario: psql -U postgres -f infra/reset-datos.sql
-- Hace falta porque un vehículo y un repartidor hacen una sola ruta por día (RN11).

\connect pedidos_db
TRUNCATE cambio_estado, pedido, cliente RESTART IDENTITY CASCADE;

\connect despacho_db
TRUNCATE parada, ruta, notificacion_pedido RESTART IDENTITY CASCADE;
-- vehículos creados durante las pruebas (los de la semilla se conservan)
DELETE FROM vehiculo WHERE patente NOT IN ('AB123CD', 'AC456EF', 'AD789GH', 'AE012IJ');
