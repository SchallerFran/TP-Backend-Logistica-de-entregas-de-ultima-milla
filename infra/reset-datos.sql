
\connect pedidos_db
TRUNCATE cambio_estado, pedido, cliente RESTART IDENTITY CASCADE;

\connect despacho_db
TRUNCATE parada, ruta, notificacion_pedido RESTART IDENTITY CASCADE;
DELETE FROM vehiculo WHERE patente NOT IN ('AB123CD', 'AC456EF', 'AD789GH', 'AE012IJ');
