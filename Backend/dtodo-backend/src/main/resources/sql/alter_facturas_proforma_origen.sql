-- Registra de forma inequívoca qué proforma originó una factura.
ALTER TABLE facturas
    ADD COLUMN IF NOT EXISTS id_proforma_origen BIGINT NULL AFTER id_sucursal;

CREATE UNIQUE INDEX IF NOT EXISTS uq_facturas_proforma_origen
    ON facturas (id_proforma_origen);

ALTER TABLE facturas
    ADD CONSTRAINT fk_facturas_proforma_origen
    FOREIGN KEY (id_proforma_origen) REFERENCES proformas (id_proforma);
