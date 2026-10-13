-- Traslados entre bodegas: un despacho puede tener como destino una sucursal O una bodega.
-- Se reutiliza el flujo de despachos (pendiente -> aprobado/cancelado, comprobante, reportes).
-- Es aditivo: los despachos existentes conservan su sucursal destino y la nueva columna queda en NULL.
-- Se puede ejecutar más de una vez.
ALTER TABLE despachos_bodega
    MODIFY id_sucursal_destino INT NULL,
    ADD COLUMN IF NOT EXISTS id_bodega_destino INT NULL AFTER id_sucursal_destino;

ALTER TABLE despachos_bodega
    ADD CONSTRAINT fk_despacho_bodega_destino FOREIGN KEY IF NOT EXISTS (id_bodega_destino) REFERENCES bodegas(id_bodega),
    ADD CONSTRAINT IF NOT EXISTS chk_despacho_un_destino CHECK ((id_sucursal_destino IS NULL) <> (id_bodega_destino IS NULL));

CREATE INDEX IF NOT EXISTS idx_despacho_bodega_destino ON despachos_bodega(id_bodega_destino);
