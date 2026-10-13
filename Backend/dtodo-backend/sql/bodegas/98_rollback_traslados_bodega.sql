-- Revierte 07_alter_despachos_bodega_traslados.sql.
-- Antes de correrlo hay que eliminar los despachos cuyo destino es una bodega (id_bodega_destino NOT NULL),
-- porque id_sucursal_destino vuelve a ser obligatorio:
--   DELETE FROM despachos_bodega_detalle WHERE id_despacho IN (SELECT id_despacho FROM despachos_bodega WHERE id_bodega_destino IS NOT NULL);
--   DELETE FROM despachos_bodega WHERE id_bodega_destino IS NOT NULL;
ALTER TABLE despachos_bodega
    DROP CONSTRAINT IF EXISTS chk_despacho_un_destino,
    DROP FOREIGN KEY IF EXISTS fk_despacho_bodega_destino;
DROP INDEX IF EXISTS idx_despacho_bodega_destino ON despachos_bodega;
ALTER TABLE despachos_bodega
    DROP COLUMN IF EXISTS id_bodega_destino,
    MODIFY id_sucursal_destino INT NOT NULL;
