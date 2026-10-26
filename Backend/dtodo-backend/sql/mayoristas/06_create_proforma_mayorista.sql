-- Módulo de Clientes Mayoristas: proforma mayorista.
-- Aditivo. Las proformas existentes quedan como MINORISTA, sin ruta, sin observaciones y sin vigencia.
-- Las columnas nuevas (ALTER TABLE) solo se aplican una vez, como en los demás scripts de este módulo.
--
--   tipo_proforma           MINORISTA (la de siempre) o MAYORISTA. Es independiente del tipo del cliente: un
--                           mayorista que compra unidades en la tienda recibe una proforma normal.
--   id_ruta                 Ruta del cliente al crear la proforma. Se copia para que los reportes históricos no
--                           cambien si el cliente cambia de ruta.
--   observaciones           Observación general (cómo va empacado el pedido). Solo se imprime en la copia de la tienda.
--   fecha_vencimiento       Vigencia de la proforma mayorista (15 días desde la emisión). Por ahora solo se guarda.
--   fecha_entrega_estimada  Fecha prometida de entrega, opcional; sirve para el indicador de entregas a tiempo.
--   proformas_detalle.observacion  Observación de la línea; se imprime debajo del producto.
ALTER TABLE proformas
    ADD COLUMN tipo_proforma VARCHAR(20) NOT NULL DEFAULT 'MINORISTA',
    ADD COLUMN id_ruta INT NULL,
    ADD COLUMN observaciones VARCHAR(500) NULL,
    ADD COLUMN fecha_vencimiento DATE NULL,
    ADD COLUMN fecha_entrega_estimada DATE NULL;

ALTER TABLE proformas
    ADD CONSTRAINT fk_proforma_ruta FOREIGN KEY (id_ruta) REFERENCES rutas(id_ruta);

CREATE INDEX idx_proforma_tipo ON proformas(tipo_proforma);

ALTER TABLE proformas_detalle ADD COLUMN observacion VARCHAR(255) NULL;
