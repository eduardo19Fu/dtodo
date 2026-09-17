CREATE TABLE compras_detalle (
    id_detalle BIGINT AUTO_INCREMENT PRIMARY KEY,
    id_compra BIGINT NOT NULL,
    id_producto INT NOT NULL,
    cantidad INT NOT NULL,
    precio_unitario DECIMAL(10,2) NOT NULL,
    sub_total DECIMAL(12,2) NOT NULL,
    CONSTRAINT fk_detalle_compra FOREIGN KEY (id_compra) REFERENCES compras(id_compra),
    CONSTRAINT fk_detalle_producto FOREIGN KEY (id_producto) REFERENCES productos(id_producto)
);

CREATE INDEX idx_detalle_compra ON compras_detalle(id_compra);
