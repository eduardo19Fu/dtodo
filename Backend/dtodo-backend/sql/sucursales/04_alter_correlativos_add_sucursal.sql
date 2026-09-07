ALTER TABLE correlativos
ADD COLUMN id_sucursal INT;

ALTER TABLE correlativos
ADD CONSTRAINT fk_correlativo_sucursal FOREIGN KEY (id_sucursal) REFERENCES sucursales(id_sucursal);
