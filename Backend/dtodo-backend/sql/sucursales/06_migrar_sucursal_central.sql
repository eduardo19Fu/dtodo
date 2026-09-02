INSERT INTO sucursales (nombre, direccion, telefono, encargado, codigo_establecimiento_sat, es_principal, fecha_registro, id_estado, id_usuario)
SELECT 'Sucursal Central',
       COALESCE((SELECT direccion FROM emisores LIMIT 1), 'Por definir'),
       NULL, NULL, 1, TRUE, NOW(),
       (SELECT id_estado FROM estados WHERE estado = 'ACTIVO' LIMIT 1),
       NULL;

SET @id_sucursal_central = (SELECT id_sucursal FROM sucursales WHERE es_principal = TRUE LIMIT 1);

UPDATE usuarios SET id_sucursal = @id_sucursal_central WHERE id_sucursal IS NULL;
UPDATE correlativos SET id_sucursal = @id_sucursal_central WHERE id_sucursal IS NULL;
UPDATE facturas SET id_sucursal = @id_sucursal_central WHERE id_sucursal IS NULL;
UPDATE proformas SET id_sucursal = @id_sucursal_central WHERE id_sucursal IS NULL;
UPDATE notas_credito SET id_sucursal = @id_sucursal_central WHERE id_sucursal IS NULL;
UPDATE movimientos_producto SET id_sucursal = @id_sucursal_central WHERE id_sucursal IS NULL;

INSERT INTO inventario_sucursal (id_sucursal, id_producto, stock, stock_minimo, fecha_actualizacion)
SELECT @id_sucursal_central, p.id_producto, p.stock, NULL, NOW()
FROM productos p;
