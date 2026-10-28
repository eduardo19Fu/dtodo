-- Módulo de Clientes Mayoristas, Fase 5 (paso 1): devoluciones de clientes.
-- Aditivo. Se aplica una sola vez, como los demás scripts de este módulo.
--
--   devoluciones_cliente           Una devolución de lo ya entregado de un pedido: pedido, cliente, fecha, motivo, valor devuelto,
--                                  sucursal que recibe el producto, la nota de crédito a la que se liga cuando el pedido está
--                                  facturado y quién la registró. Solo el administrador las registra, y quedan aprobadas al
--                                  registrarlas.
--   devoluciones_cliente_detalle   Por línea del pedido: unidades devueltas, destino (REINGRESO al inventario de la sucursal o
--                                  BAJA por merma) y valor devuelto (unidades x importe de la línea con descuento / unidades).
--   proformas_detalle.cantidad_devuelta   Lo ya devuelto de cada línea: lo devolvible es lo entregado menos lo devuelto.
--
-- movimientos_producto.tipo_movimiento es un ENUM de MySQL: se agrega DEVOLUCION_CLIENTE (entrada al inventario) solo si
-- falta, igual que en el script 10.
SET @tipo_actual = (SELECT COLUMN_TYPE FROM information_schema.COLUMNS
                     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'movimientos_producto' AND COLUMN_NAME = 'tipo_movimiento');
SET @tipo_nuevo = @tipo_actual;
SET @tipo_nuevo = IF(LEFT(@tipo_nuevo, 5) <> 'enum(' OR LOCATE('''DEVOLUCION_CLIENTE''', @tipo_nuevo) > 0, @tipo_nuevo,
                     CONCAT(SUBSTRING(@tipo_nuevo, 1, CHAR_LENGTH(@tipo_nuevo) - 1), ',''DEVOLUCION_CLIENTE'')'));
SET @ampliar = IF(@tipo_nuevo = @tipo_actual, 'SELECT 1',
                  CONCAT('ALTER TABLE movimientos_producto MODIFY tipo_movimiento ', @tipo_nuevo, ' NULL'));
PREPARE ampliar_tipos FROM @ampliar;
EXECUTE ampliar_tipos;
DEALLOCATE PREPARE ampliar_tipos;

ALTER TABLE proformas_detalle
    ADD COLUMN cantidad_devuelta INT NOT NULL DEFAULT 0;

CREATE TABLE devoluciones_cliente (
    id_devolucion BIGINT AUTO_INCREMENT PRIMARY KEY,
    id_proforma BIGINT NOT NULL,
    id_cliente INT NOT NULL,
    id_sucursal INT NULL,
    id_nota_credito INT NULL,
    fecha DATE NOT NULL,
    motivo VARCHAR(500) NOT NULL,
    total DECIMAL(12,2) NOT NULL,
    id_usuario INT NOT NULL,
    fecha_registro DATETIME NOT NULL,
    CONSTRAINT fk_devcli_cliente FOREIGN KEY (id_cliente) REFERENCES clientes(id_cliente),
    CONSTRAINT fk_devcli_sucursal FOREIGN KEY (id_sucursal) REFERENCES sucursales(id_sucursal),
    CONSTRAINT fk_devcli_nota FOREIGN KEY (id_nota_credito) REFERENCES notas_credito(id_nota_credito),
    CONSTRAINT fk_devcli_usuario FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario)
);

CREATE INDEX idx_devcli_proforma ON devoluciones_cliente(id_proforma);
CREATE INDEX idx_devcli_cliente ON devoluciones_cliente(id_cliente, fecha);

CREATE TABLE devoluciones_cliente_detalle (
    id_devolucion_detalle BIGINT AUTO_INCREMENT PRIMARY KEY,
    id_devolucion BIGINT NOT NULL,
    id_detalle BIGINT NOT NULL,
    cantidad INT NOT NULL,
    destino VARCHAR(10) NOT NULL,
    valor DECIMAL(12,2) NOT NULL,
    CONSTRAINT fk_devcli_det_devolucion FOREIGN KEY (id_devolucion) REFERENCES devoluciones_cliente(id_devolucion)
);

CREATE INDEX idx_devcli_det_devolucion ON devoluciones_cliente_detalle(id_devolucion);
