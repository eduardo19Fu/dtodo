CREATE PROCEDURE `sp_consultar_productos_dto`(IN idEstado INT)
BEGIN
    IF idEstado = 0 THEN
        SELECT  prod.id_producto as idProducto,
                prod.cod_producto as codProducto,
                prod.precio_compra as precioCompra,
                prod.precio_venta as precioVenta,
                prod.stock,
                prod.nombre,
                mp.marca as marcaProducto,
                tp.tipo_producto as tipoProducto,
                prod.porcentaje_ganancia as porcentajeGanancia,
                prod.descripcion,
                prod.fecha_ingreso as fechaIngreso,
                prod.fecha_registro as fechaRegistro,
                e.estado
        FROM productos as prod
        INNER JOIN marcas_producto as mp ON mp.id_marca_producto = prod.id_marca_producto
        INNER JOIN tipos_producto as tp ON tp.id_tipo_producto = prod.id_tipo_producto
        INNER JOIN estados as e ON e.id_estado = prod.id_estado
        ORDER BY prod.nombre ASC;
    ELSE
        SELECT  prod.id_producto as idProducto,
                prod.cod_producto as codProducto,
                prod.precio_compra as precioCompra,
                prod.precio_venta as precioVenta,
                prod.stock,
                prod.nombre,
                mp.marca as marcaProducto,
                tp.tipo_producto as tipoProducto,
                prod.porcentaje_ganancia as porcentajeGanancia,
                prod.descripcion,
                prod.fecha_ingreso as fechaIngreso,
                prod.fecha_registro as fechaRegistro,
                e.estado
        FROM productos as prod
        INNER JOIN marcas_producto as mp ON mp.id_marca_producto = prod.id_marca_producto
        INNER JOIN tipos_producto as tp ON tp.id_tipo_producto = prod.id_tipo_producto
        INNER JOIN estados as e ON e.id_estado = prod.id_estado
        WHERE prod.id_estado = idEstado
        ORDER BY prod.nombre ASC;
    END IF;
END