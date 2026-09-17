CREATE TABLE compras (
    id_compra BIGINT AUTO_INCREMENT PRIMARY KEY,
    fecha_compra DATE NOT NULL,
    fecha_registro DATETIME NOT NULL,
    no_comprobante VARCHAR(50) NOT NULL,
    tipo_comprobante VARCHAR(30) NOT NULL,
    total DECIMAL(12,2) NOT NULL DEFAULT 0,
    costo_envio DECIMAL(12,2) NOT NULL DEFAULT 0,
    observaciones VARCHAR(500),
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVA',
    id_proveedor INT NOT NULL,
    id_usuario INT NOT NULL,
    id_sucursal INT NOT NULL,
    CONSTRAINT fk_compra_proveedor FOREIGN KEY (id_proveedor) REFERENCES proveedores(id_proveedor),
    CONSTRAINT fk_compra_usuario FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario),
    CONSTRAINT fk_compra_sucursal FOREIGN KEY (id_sucursal) REFERENCES sucursales(id_sucursal)
);

CREATE INDEX idx_compra_sucursal ON compras(id_sucursal);
CREATE INDEX idx_compra_proveedor ON compras(id_proveedor);
CREATE INDEX idx_compra_fecha ON compras(fecha_compra);
