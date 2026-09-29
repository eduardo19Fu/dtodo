-- Aplicar antes de desplegar el backend que registra referencias de documentos.
-- Los movimientos anteriores quedan sin vínculo: no se puede inferir una factura
-- únicamente por fecha, producto y cantidad sin riesgo de asignar otra venta.
ALTER TABLE movimientos_producto
    ADD COLUMN tipo_documento_origen VARCHAR(30) NULL,
    ADD COLUMN id_documento_origen BIGINT NULL;

CREATE INDEX idx_movimientos_documento_origen
    ON movimientos_producto (tipo_documento_origen, id_documento_origen);
