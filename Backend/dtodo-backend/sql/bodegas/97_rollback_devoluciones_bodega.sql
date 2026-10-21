-- Revierte 08_create_devoluciones_bodega.sql. DESTRUCTIVO: borra todas las devoluciones y su detalle.
-- No deshace los movimientos que las devoluciones ya aplicaron al inventario de sucursal ni de bodega
-- (movimientos_producto y movimientos_bodega conservan sus filas con tipo_documento_origen = 'DEVOLUCION_BODEGA').
DROP TABLE IF EXISTS devoluciones_bodega_detalle;
DROP TABLE IF EXISTS devoluciones_bodega;
