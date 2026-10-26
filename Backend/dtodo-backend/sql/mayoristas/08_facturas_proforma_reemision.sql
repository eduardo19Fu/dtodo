-- Módulo de Clientes Mayoristas: una proforma puede volver a facturarse después de anular su factura.
-- El índice único sobre facturas.id_proforma_origen permitía una sola factura por proforma, anulada o no, y
-- bloqueaba la reemisión (por ejemplo, con el NIT correcto). Desde ahora el servidor impide dos facturas vigentes
-- de la misma proforma (bloqueando la proforma al emitir) y las anuladas ya no cuentan.
--
-- Primero el índice nuevo: la llave foránea fk_facturas_proforma_origen necesita un índice, y MySQL no deja
-- borrar el único que la respalda mientras no haya otro.
-- Se aplica una sola vez, como los demás scripts de este módulo.
CREATE INDEX idx_facturas_proforma_origen ON facturas (id_proforma_origen);
DROP INDEX uq_facturas_proforma_origen ON facturas;
