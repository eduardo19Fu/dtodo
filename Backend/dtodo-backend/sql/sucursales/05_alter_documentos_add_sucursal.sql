ALTER TABLE facturas
ADD COLUMN id_sucursal INT;

ALTER TABLE facturas
ADD CONSTRAINT fk_factura_sucursal FOREIGN KEY (id_sucursal) REFERENCES sucursales(id_sucursal);

ALTER TABLE proformas
ADD COLUMN id_sucursal INT;

ALTER TABLE proformas
ADD CONSTRAINT fk_proforma_sucursal FOREIGN KEY (id_sucursal) REFERENCES sucursales(id_sucursal);

ALTER TABLE notas_credito
ADD COLUMN id_sucursal INT;

ALTER TABLE notas_credito
ADD CONSTRAINT fk_nota_credito_sucursal FOREIGN KEY (id_sucursal) REFERENCES sucursales(id_sucursal);

ALTER TABLE movimientos_producto
ADD COLUMN id_sucursal INT;

ALTER TABLE movimientos_producto
ADD CONSTRAINT fk_movimiento_sucursal FOREIGN KEY (id_sucursal) REFERENCES sucursales(id_sucursal);
