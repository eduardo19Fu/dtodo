-- Módulo de Clientes Mayoristas, Fase 4 (paso 2): condición de pago del pedido.
-- Aditivo. Las proformas existentes quedan de CONTADO.
--
--   condicion_pago   CONTADO (se cobra con un recibo de caja antes de entregar) o CREDITO (solo para un cliente con
--                    límite y días de crédito).
--   dias_credito     Los días de crédito del cliente al momento de hacer el pedido; los días corren desde la entrega.
ALTER TABLE proformas
    ADD COLUMN condicion_pago VARCHAR(10) NOT NULL DEFAULT 'CONTADO',
    ADD COLUMN dias_credito INT NULL;
