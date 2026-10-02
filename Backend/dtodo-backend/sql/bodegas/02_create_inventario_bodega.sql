-- Existencias por producto × bodega (equivalente a inventario_sucursal, pero independiente de él).
CREATE TABLE inventario_bodega (
    id_inventario_bodega BIGINT AUTO_INCREMENT PRIMARY KEY,
    id_bodega INT NOT NULL,
    id_producto INT NOT NULL,
    stock INT NOT NULL DEFAULT 0,
    stock_minimo INT,
    fecha_actualizacion DATETIME NOT NULL,
    CONSTRAINT fk_inventario_bodega_bodega FOREIGN KEY (id_bodega) REFERENCES bodegas(id_bodega),
    CONSTRAINT fk_inventario_bodega_producto FOREIGN KEY (id_producto) REFERENCES productos(id_producto),
    CONSTRAINT uq_inventario_bodega_producto UNIQUE (id_bodega, id_producto),
    CONSTRAINT chk_inventario_bodega_stock CHECK (stock >= 0)
);

CREATE INDEX idx_inventario_bodega_producto ON inventario_bodega(id_producto);
