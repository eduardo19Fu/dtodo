DROP PROCEDURE IF EXISTS `sp_consultar_productos`;

DELIMITER $$

CREATE PROCEDURE `sp_consultar_productos`(IN idEstado INT, IN idSucursal INT)
BEGIN
    IF idEstado = 0 THEN
        SELECT p.id_producto,
               p.cod_producto,
               p.nombre,
               p.id_tipo_producto,
               p.id_marca_producto,
               p.precio_compra,
               p.precio_venta,
               p.porcentaje_ganancia,
               p.fecha_ingreso,
               p.fecha_vencimiento,
               p.id_estado,
               COALESCE(inv.stock, 0) AS stock,
               p.imagen,
               p.fecha_registro,
               p.descripcion,
               p.link
        FROM productos p
        LEFT JOIN inventario_sucursal inv ON inv.id_producto = p.id_producto AND inv.id_sucursal = idSucursal
        ORDER BY p.nombre;
    ELSE
        SELECT p.id_producto,
               p.cod_producto,
               p.nombre,
               p.id_tipo_producto,
               p.id_marca_producto,
               p.precio_compra,
               p.precio_venta,
               p.porcentaje_ganancia,
               p.fecha_ingreso,
               p.fecha_vencimiento,
               p.id_estado,
               COALESCE(inv.stock, 0) AS stock,
               p.imagen,
               p.fecha_registro,
               p.descripcion,
               p.link
        FROM productos p
        LEFT JOIN inventario_sucursal inv ON inv.id_producto = p.id_producto AND inv.id_sucursal = idSucursal
        WHERE p.id_estado = idEstado
        ORDER BY p.nombre;
    END IF;
END$$

DELIMITER ;
