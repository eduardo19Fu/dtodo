-- Permite volver a facturar una proforma cuya factura fue anulada (p. ej. se emitió con el NIT incorrecto).
-- El índice único sobre facturas.id_proforma_origen admitía una sola factura por proforma, anulada o no.
-- Desde ahora el servidor impide dos facturas vigentes de la misma proforma (bloqueando la proforma al emitir).
--
-- Primero el índice normal: la llave foránea fk_facturas_proforma_origen necesita un índice, y no se puede
-- borrar el único que la respalda mientras no exista otro. Es idempotente (MariaDB).
-- Aplicar ANTES de desplegar el jar.
CREATE INDEX IF NOT EXISTS idx_facturas_proforma_origen ON facturas (id_proforma_origen);
DROP INDEX IF EXISTS uq_facturas_proforma_origen ON facturas;
