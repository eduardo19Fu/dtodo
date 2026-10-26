-- Módulo de Clientes Mayoristas: descuentos y precio mayorista.
-- Aditivo. Las tablas y los porcentajes iniciales se pueden repetir sin duplicar; las columnas nuevas (ALTER TABLE)
-- solo se aplican una vez, como en los demás scripts de este módulo.
--
-- Cómo se descuenta a un mayorista (el usuario elige el tipo en cada línea de la proforma):
--   CATEGORIA         porcentaje de la categoría del cliente (A, B o VOLUMEN); Volumen tiene varios niveles.
--   CIENTO / FARDO    porcentaje especial de un producto cuando se compra la cantidad mínima.
--   PRECIO_MAYORISTA  precio fijo de un producto; se guarda como el porcentaje equivalente sobre precio_venta,
--                     de modo que factura, reportes e impresión no necesitan conocerlo.
--   MANUAL            porcentaje libre.

-- 1) Niveles de descuento por categoría de cliente. Configurables: A y B tienen uno; Volumen, varios, y el
--    vendedor elige cuál aplica (ningún nivel se aplica solo).
CREATE TABLE IF NOT EXISTS categorias_descuento_niveles (
    id_nivel INT AUTO_INCREMENT PRIMARY KEY,
    categoria VARCHAR(20) NOT NULL,
    porcentaje DECIMAL(5,2) NOT NULL,
    activo TINYINT(1) NOT NULL DEFAULT 1,
    CONSTRAINT uq_categoria_nivel UNIQUE (categoria, porcentaje)
);

INSERT INTO categorias_descuento_niveles (categoria, porcentaje, activo)
SELECT datos.categoria, datos.porcentaje, 1
FROM (
    SELECT 'A' AS categoria, 10.00 AS porcentaje
    UNION ALL SELECT 'B', 8.00
    UNION ALL SELECT 'VOLUMEN', 12.00
    UNION ALL SELECT 'VOLUMEN', 15.00
    UNION ALL SELECT 'VOLUMEN', 20.00
) datos
WHERE NOT EXISTS (
    SELECT 1 FROM categorias_descuento_niveles n
    WHERE n.categoria = datos.categoria AND n.porcentaje = datos.porcentaje
);

-- 2) Precio mayorista fijo, opcional por producto. Lo llenan los usuarios solo en los productos que quieran.
--    Va en una tabla aparte y no como columna de productos: el procedimiento sp_consultar_productos devuelve las
--    columnas de productos y se mapea directo a la entidad, de modo que una columna nueva lo rompería.
--    Algunos productos ya tienen otro código con el precio por caja o ciento; ese esquema sigue funcionando.
CREATE TABLE IF NOT EXISTS precios_mayorista (
    id_producto INT PRIMARY KEY,
    precio DECIMAL(12,2) NOT NULL,
    CONSTRAINT fk_precio_mayorista_producto FOREIGN KEY (id_producto) REFERENCES productos(id_producto)
);

-- 3) Descuentos especiales por producto: ciento o fardo cerrado, con la cantidad mínima y el porcentaje.
--    Para FARDO, cantidad_minima son las unidades que trae el fardo de ese producto.
CREATE TABLE IF NOT EXISTS descuentos_producto (
    id_descuento INT AUTO_INCREMENT PRIMARY KEY,
    id_producto INT NOT NULL,
    tipo VARCHAR(20) NOT NULL,
    cantidad_minima INT NOT NULL,
    porcentaje DECIMAL(5,2) NOT NULL,
    activo TINYINT(1) NOT NULL DEFAULT 1,
    CONSTRAINT uq_descuento_producto_tipo UNIQUE (id_producto, tipo),
    CONSTRAINT fk_descuento_producto FOREIGN KEY (id_producto) REFERENCES productos(id_producto)
);

-- 4) La línea de la proforma recuerda de dónde salió su descuento y el precio unitario de ese momento
--    (el precio histórico no se guardaba y cambia cuando se actualiza el producto).
--    NULL en las líneas anteriores a este cambio.
ALTER TABLE proformas_detalle
    ADD COLUMN precio_unitario DECIMAL(12,2) NULL,
    ADD COLUMN tipo_descuento VARCHAR(20) NULL,
    ADD COLUMN porcentaje_sugerido DECIMAL(7,4) NULL;
