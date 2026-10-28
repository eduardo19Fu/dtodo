-- Módulo de Clientes Mayoristas, Fase 5: las comisiones son netas de devoluciones.
-- Aditivo. Las liquidaciones que ya existan quedan con monto devuelto 0 (se liquidaron sobre lo entregado).
--
--   comisiones_liquidaciones.monto_devuelto   Lo que los clientes devolvieron de los pedidos del vendedor en el período de su
--                                             comisión. La comisión se calcula sobre lo entregado menos esto (nunca menos de cero);
--                                             se guarda para que un año liquidado muestre cómo se calculó y no se recalcule.
ALTER TABLE comisiones_liquidaciones
    ADD COLUMN monto_devuelto DECIMAL(14,2) NOT NULL DEFAULT 0;
