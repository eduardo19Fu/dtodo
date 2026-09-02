ALTER TABLE usuarios
ADD COLUMN id_sucursal INT;

ALTER TABLE usuarios
ADD CONSTRAINT fk_usuario_sucursal FOREIGN KEY (id_sucursal) REFERENCES sucursales(id_sucursal);
