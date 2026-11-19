-- Módulo de Clientes Mayoristas: modalidad de entrega y cotizaciones.
-- Aditivo. Se aplica una sola vez, como los demás scripts de este módulo.
--
--   proformas.modalidad_entrega  Cómo recibe el cliente el pedido: RUTA (la de siempre: sale con el piloto de la ruta), TIENDA
--                                (recoge en la tienda) o TRANSPORTE (se envía por un transporte ajeno a la empresa).
--   proformas.transporte         Nombre del transporte (y guía, si la hay) cuando la modalidad es TRANSPORTE.
--   proformas.es_cotizacion      1 si es solo una cotización (precios): no es un pedido, no pasa por bodega, no entra a la cobranza ni
--                                se factura. Se convierte en pedido cuando el cliente confirma.
ALTER TABLE proformas
    ADD COLUMN modalidad_entrega VARCHAR(20) NOT NULL DEFAULT 'RUTA',
    ADD COLUMN transporte VARCHAR(120) NULL,
    ADD COLUMN es_cotizacion TINYINT(1) NOT NULL DEFAULT 0;
