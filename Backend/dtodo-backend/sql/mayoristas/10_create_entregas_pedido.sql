-- Módulo de Clientes Mayoristas, Fase 2B: entrega del pedido mayorista y su descuento de inventario.
-- Aditivo. Las columnas nuevas (ALTER TABLE) solo se aplican una vez, como en los demás scripts de este módulo.
--
--   proformas_detalle.cantidad_entregada  Unidades entregadas físicamente al cliente (suma de sus entregas no anuladas).
--   proformas_detalle.cantidad_descontada Unidades de la línea que ya salieron del inventario por este pedido, ya sea
--                                         por una entrega, por una salida manual declarada en la entrega o (paso
--                                         siguiente de la 2B) por la factura. Evita descontar dos veces.
--   proformas_entregas                    Cada entrega: sucursal de la que se descuenta, quién la registró, fecha real
--                                         de entrega y fecha de registro (distintas a propósito), y si el inventario
--                                         ya se había descontado a mano. Una entrega anulada se conserva.
--   proformas_entregas_detalle            Unidades entregadas por línea y cuántas descontó el sistema.
--
-- movimientos_producto.tipo_movimiento es un ENUM de MySQL: un valor que no esté en la lista falla al guardar. Se
-- agregan los tipos nuevos (ENTREGA_PROFORMA, ANULACION_ENTREGA_PROFORMA) y también ENTREGA_PRODUCTO_NOTA, que el
-- código ya usa para las notas de crédito pero no estaba en la lista de esta base. El bloque lee la lista actual y
-- agrega solo lo que falta, al final: sirve igual si la base ya tiene alguno de esos valores. Si la columna no es un
-- ENUM no hace nada.
SET @tipo_actual = (SELECT COLUMN_TYPE FROM information_schema.COLUMNS
                     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'movimientos_producto' AND COLUMN_NAME = 'tipo_movimiento');
SET @tipo_nuevo = @tipo_actual;
SET @tipo_nuevo = IF(LEFT(@tipo_nuevo, 5) <> 'enum(' OR LOCATE('''ENTREGA_PRODUCTO_NOTA''', @tipo_nuevo) > 0, @tipo_nuevo,
                     CONCAT(SUBSTRING(@tipo_nuevo, 1, CHAR_LENGTH(@tipo_nuevo) - 1), ',''ENTREGA_PRODUCTO_NOTA'')'));
SET @tipo_nuevo = IF(LEFT(@tipo_nuevo, 5) <> 'enum(' OR LOCATE('''ENTREGA_PROFORMA''', @tipo_nuevo) > 0, @tipo_nuevo,
                     CONCAT(SUBSTRING(@tipo_nuevo, 1, CHAR_LENGTH(@tipo_nuevo) - 1), ',''ENTREGA_PROFORMA'')'));
SET @tipo_nuevo = IF(LEFT(@tipo_nuevo, 5) <> 'enum(' OR LOCATE('''ANULACION_ENTREGA_PROFORMA''', @tipo_nuevo) > 0, @tipo_nuevo,
                     CONCAT(SUBSTRING(@tipo_nuevo, 1, CHAR_LENGTH(@tipo_nuevo) - 1), ',''ANULACION_ENTREGA_PROFORMA'')'));
SET @ampliar = IF(@tipo_nuevo = @tipo_actual, 'SELECT 1',
                  CONCAT('ALTER TABLE movimientos_producto MODIFY tipo_movimiento ', @tipo_nuevo, ' NULL'));
PREPARE ampliar_tipos FROM @ampliar;
EXECUTE ampliar_tipos;
DEALLOCATE PREPARE ampliar_tipos;

ALTER TABLE proformas_detalle
    ADD COLUMN cantidad_entregada INT NOT NULL DEFAULT 0,
    ADD COLUMN cantidad_descontada INT NOT NULL DEFAULT 0;

CREATE TABLE proformas_entregas (
    id_entrega BIGINT AUTO_INCREMENT PRIMARY KEY,
    id_proforma BIGINT NOT NULL,
    id_sucursal INT NOT NULL,
    id_usuario INT NOT NULL,
    entregado_por VARCHAR(150) NULL,
    fecha_entrega DATE NOT NULL,
    fecha_registro DATETIME NOT NULL,
    descontado_a_mano TINYINT(1) NOT NULL DEFAULT 0,
    observaciones VARCHAR(255) NULL,
    anulada TINYINT(1) NOT NULL DEFAULT 0,
    fecha_anulacion DATETIME NULL,
    id_usuario_anulacion INT NULL,
    motivo_anulacion VARCHAR(500) NULL,
    CONSTRAINT fk_proforma_entrega_sucursal FOREIGN KEY (id_sucursal) REFERENCES sucursales(id_sucursal),
    CONSTRAINT fk_proforma_entrega_usuario FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario),
    CONSTRAINT fk_proforma_entrega_usuario_anula FOREIGN KEY (id_usuario_anulacion) REFERENCES usuarios(id_usuario)
);

CREATE INDEX idx_proforma_entrega ON proformas_entregas (id_proforma, fecha_entrega);

CREATE TABLE proformas_entregas_detalle (
    id_entrega_detalle BIGINT AUTO_INCREMENT PRIMARY KEY,
    id_entrega BIGINT NOT NULL,
    id_detalle BIGINT NOT NULL,
    cantidad INT NOT NULL,
    cantidad_cubierta INT NOT NULL DEFAULT 0,
    cantidad_descontada INT NOT NULL DEFAULT 0,
    CONSTRAINT fk_entrega_detalle_entrega FOREIGN KEY (id_entrega) REFERENCES proformas_entregas(id_entrega)
);

CREATE INDEX idx_entrega_detalle_entrega ON proformas_entregas_detalle (id_entrega);
