-- Módulo de Clientes Mayoristas, Fase 5: anulación de devoluciones de clientes.
-- Aditivo. Las devoluciones que ya existan quedan vigentes (anulada = 0).
--
--   devoluciones_cliente.anulada / fecha_anulacion / id_usuario_anulacion / motivo_anulacion
--       Una devolución anulada se conserva con quién, cuándo y por qué, y deja de contar: vuelve a subir lo que debe el cliente
--       y lo devolvible de cada línea, y lo que reingresó al inventario sale de nuevo.
--
-- movimientos_producto.tipo_movimiento es un ENUM de MySQL: se agrega ANULACION_DEVOLUCION_CLIENTE (salida del inventario)
-- solo si falta, igual que en los scripts 10 y 18.
SET @tipo_actual = (SELECT COLUMN_TYPE FROM information_schema.COLUMNS
                     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'movimientos_producto' AND COLUMN_NAME = 'tipo_movimiento');
SET @tipo_nuevo = @tipo_actual;
SET @tipo_nuevo = IF(LEFT(@tipo_nuevo, 5) <> 'enum(' OR LOCATE('''ANULACION_DEVOLUCION_CLIENTE''', @tipo_nuevo) > 0, @tipo_nuevo,
                     CONCAT(SUBSTRING(@tipo_nuevo, 1, CHAR_LENGTH(@tipo_nuevo) - 1), ',''ANULACION_DEVOLUCION_CLIENTE'')'));
SET @ampliar = IF(@tipo_nuevo = @tipo_actual, 'SELECT 1',
                  CONCAT('ALTER TABLE movimientos_producto MODIFY tipo_movimiento ', @tipo_nuevo, ' NULL'));
PREPARE ampliar_tipos FROM @ampliar;
EXECUTE ampliar_tipos;
DEALLOCATE PREPARE ampliar_tipos;

ALTER TABLE devoluciones_cliente
    ADD COLUMN anulada TINYINT(1) NOT NULL DEFAULT 0,
    ADD COLUMN fecha_anulacion DATETIME NULL,
    ADD COLUMN id_usuario_anulacion INT NULL,
    ADD COLUMN motivo_anulacion VARCHAR(500) NULL,
    ADD CONSTRAINT fk_devcli_usuario_anula FOREIGN KEY (id_usuario_anulacion) REFERENCES usuarios(id_usuario);
