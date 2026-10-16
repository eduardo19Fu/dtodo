-- Devoluciones de una sucursal (tienda) hacia una bodega.
-- estado: PENDIENTE (pendiente de aprobación), REALIZADO (aprobada), CANCELADO (rechazada o cancelada);
-- los valores los controla EstadoDespachoBodegaEnum, igual que en los despachos.
-- El stock sale de la sucursal al registrar la devolución (queda reservado) y entra a la bodega cuando un
-- administrador la aprueba; si se rechaza o cancela estando pendiente, regresa a la sucursal.
-- Es aditivo: no altera ninguna tabla existente. Los movimientos de bodega nuevos usan el tipo
-- INGRESO_DEVOLUCION, que cabe en movimientos_bodega.tipo_movimiento (VARCHAR) sin alterar la tabla.
-- Se puede ejecutar más de una vez.
CREATE TABLE IF NOT EXISTS devoluciones_bodega (
    id_devolucion BIGINT AUTO_INCREMENT PRIMARY KEY,
    fecha_registro DATETIME NOT NULL,
    fecha_resolucion DATETIME NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    total DECIMAL(12,2) NOT NULL DEFAULT 0,
    entregado_por VARCHAR(150) NOT NULL,
    motivo VARCHAR(300) NOT NULL,
    observaciones VARCHAR(500),
    motivo_cancelacion VARCHAR(300),
    id_sucursal_origen INT NOT NULL,
    id_bodega INT NOT NULL,
    id_usuario_registra INT NOT NULL,
    id_usuario_resuelve INT NULL,
    CONSTRAINT fk_devolucion_sucursal FOREIGN KEY (id_sucursal_origen) REFERENCES sucursales(id_sucursal),
    CONSTRAINT fk_devolucion_bodega FOREIGN KEY (id_bodega) REFERENCES bodegas(id_bodega),
    CONSTRAINT fk_devolucion_usuario_registra FOREIGN KEY (id_usuario_registra) REFERENCES usuarios(id_usuario),
    CONSTRAINT fk_devolucion_usuario_resuelve FOREIGN KEY (id_usuario_resuelve) REFERENCES usuarios(id_usuario)
);

CREATE INDEX IF NOT EXISTS idx_devolucion_bodega ON devoluciones_bodega(id_bodega);
CREATE INDEX IF NOT EXISTS idx_devolucion_sucursal ON devoluciones_bodega(id_sucursal_origen);
CREATE INDEX IF NOT EXISTS idx_devolucion_estado_fecha ON devoluciones_bodega(estado, fecha_registro);

-- id_devolucion queda NULL en el INSERT: Hibernate inserta el detalle sin la FK y la completa con un UPDATE
-- posterior (@OneToMany unidireccional con @JoinColumn), igual que despachos_bodega_detalle.
-- precio_unitario es el costo del producto al momento de la devolución y existencia_sucursal la existencia de
-- la sucursal justo antes de descontar: ambos se guardan para que el documento sea reproducible en el tiempo.
CREATE TABLE IF NOT EXISTS devoluciones_bodega_detalle (
    id_detalle BIGINT AUTO_INCREMENT PRIMARY KEY,
    id_devolucion BIGINT NULL,
    id_producto INT NOT NULL,
    cantidad INT NOT NULL,
    precio_unitario DECIMAL(10,2) NOT NULL,
    sub_total DECIMAL(12,2) NOT NULL,
    existencia_sucursal INT NOT NULL,
    CONSTRAINT fk_devolucion_detalle_devolucion FOREIGN KEY (id_devolucion) REFERENCES devoluciones_bodega(id_devolucion),
    CONSTRAINT fk_devolucion_detalle_producto FOREIGN KEY (id_producto) REFERENCES productos(id_producto)
);

CREATE INDEX IF NOT EXISTS idx_devolucion_detalle_devolucion ON devoluciones_bodega_detalle(id_devolucion);
