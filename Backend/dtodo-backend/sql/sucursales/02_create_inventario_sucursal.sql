CREATE TABLE inventario_sucursal (
    id_inventario_sucursal BIGINT AUTO_INCREMENT PRIMARY KEY,
    id_sucursal INT NOT NULL,
    id_producto INT NOT NULL,
    stock INT NOT NULL DEFAULT 0,
    stock_minimo INT,
    fecha_actualizacion DATETIME NOT NULL,
    CONSTRAINT fk_inventario_sucursal_sucursal FOREIGN KEY (id_sucursal) REFERENCES sucursales(id_sucursal),
    CONSTRAINT fk_inventario_sucursal_producto FOREIGN KEY (id_producto) REFERENCES productos(id_producto),
    CONSTRAINT uq_inventario_sucursal_producto UNIQUE (id_sucursal, id_producto)
);

CREATE INDEX idx_inventario_sucursal_sucursal ON inventario_sucursal(id_sucursal);
CREATE INDEX idx_inventario_sucursal_producto ON inventario_sucursal(id_producto);
