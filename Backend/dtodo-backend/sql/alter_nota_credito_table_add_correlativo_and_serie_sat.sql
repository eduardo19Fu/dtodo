ALTER TABLE notas_credito
ADD COLUMN correlativo_factura_sat VARCHAR(25) AFTER id_nota_credito;

ALTER TABLE notas_credito
ADD COLUMN serie_factura_sat VARCHAR(200) AFTER correlativo_factura_sat;

ALTER TABLE notas_credito
ADD COLUMN fecha_entrega_estimada DATE AFTER fecha_creacion;

ALTER TABLE notas_credito
ADD COLUMN observaciones VARCHAR(500) AFTER restante;

ALTER TABLE notas_credito DROP COLUMN abono;
ALTER TABLE notas_credito DROP COLUMN restante;
ALTER TABLE notas_credito DROP COLUMN fecha_pago_limite;