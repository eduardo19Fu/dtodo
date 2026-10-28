-- Módulo de Clientes Mayoristas, Fase 4 (paso 3): control del límite de crédito.
-- Aditivo. Las proformas existentes quedan sin autorización de crédito pendiente.
--
--   credito_autorizacion   NULL si el pedido no pasa del límite de crédito del cliente; PENDIENTE (espera que un administrador lo
--                          autorice), APROBADA o RECHAZADA. Es independiente de la autorización del descuento.
--   exceso_credito         Cuánto pasa del límite cuando se evaluó (saldo + pedidos a crédito por entregar + este pedido - límite).
--                          Una edición que no pase de ese exceso conserva la aprobación.
ALTER TABLE proformas
    ADD COLUMN credito_autorizacion VARCHAR(12) NULL,
    ADD COLUMN exceso_credito DECIMAL(12,2) NULL;
