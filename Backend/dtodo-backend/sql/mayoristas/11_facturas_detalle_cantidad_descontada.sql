-- Módulo de Clientes Mayoristas, Fase 2B (paso 2): la factura de un pedido mayorista descuenta solo lo que el pedido
-- todavía no había descontado (lo que la entrega ya sacó del inventario no se descuenta otra vez), y al anularla
-- devuelve solo lo que ella descontó.
--
--   facturas_detalle.cantidad_descontada  Unidades que esta línea de factura descontó del inventario. NULL en las
--                                         facturas anteriores a este control y en las ventas que no vienen de un pedido
--                                         mayorista: ahí se descontó y se devuelve la cantidad completa, como siempre.
-- Se aplica una sola vez, como los demás scripts de este módulo.
ALTER TABLE facturas_detalle
    ADD COLUMN cantidad_descontada INT NULL;
