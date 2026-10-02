-- id_despacho queda NULL: Hibernate inserta el detalle sin la FK y la completa con un UPDATE posterior
-- (@OneToMany unidireccional con @JoinColumn), igual que compras_detalle.id_compra.
-- precio_unitario es el costo del producto al momento del despacho y existencia_bodega la existencia de la
-- bodega justo antes de descontar: ambos se guardan para que el comprobante sea reproducible en el tiempo.
CREATE TABLE despachos_bodega_detalle (
    id_detalle BIGINT AUTO_INCREMENT PRIMARY KEY,
    id_despacho BIGINT NULL,
    id_producto INT NOT NULL,
    cantidad INT NOT NULL,
    precio_unitario DECIMAL(10,2) NOT NULL,
    sub_total DECIMAL(12,2) NOT NULL,
    existencia_bodega INT NOT NULL,
    CONSTRAINT fk_despacho_detalle_despacho FOREIGN KEY (id_despacho) REFERENCES despachos_bodega(id_despacho),
    CONSTRAINT fk_despacho_detalle_producto FOREIGN KEY (id_producto) REFERENCES productos(id_producto)
);

CREATE INDEX idx_despacho_detalle_despacho ON despachos_bodega_detalle(id_despacho);
