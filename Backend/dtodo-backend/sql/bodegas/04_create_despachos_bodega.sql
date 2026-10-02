-- Despachos de bodega hacia una sucursal.
-- estado: PENDIENTE (pendiente de aprobación), REALIZADO, CANCELADO (controlado por EstadoDespachoBodegaEnum).
-- El stock sale de la bodega al registrar el despacho (queda reservado) y entra a la sucursal al aprobarlo.
CREATE TABLE despachos_bodega (
    id_despacho BIGINT AUTO_INCREMENT PRIMARY KEY,
    fecha_registro DATETIME NOT NULL,
    fecha_resolucion DATETIME NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    total DECIMAL(12,2) NOT NULL DEFAULT 0,
    recibido_por VARCHAR(150) NOT NULL,
    observaciones VARCHAR(500),
    motivo_cancelacion VARCHAR(300),
    id_bodega INT NOT NULL,
    id_sucursal_destino INT NOT NULL,
    id_usuario_despacha INT NOT NULL,
    id_usuario_resuelve INT NULL,
    CONSTRAINT fk_despacho_bodega FOREIGN KEY (id_bodega) REFERENCES bodegas(id_bodega),
    CONSTRAINT fk_despacho_sucursal FOREIGN KEY (id_sucursal_destino) REFERENCES sucursales(id_sucursal),
    CONSTRAINT fk_despacho_usuario_despacha FOREIGN KEY (id_usuario_despacha) REFERENCES usuarios(id_usuario),
    CONSTRAINT fk_despacho_usuario_resuelve FOREIGN KEY (id_usuario_resuelve) REFERENCES usuarios(id_usuario)
);

CREATE INDEX idx_despacho_bodega ON despachos_bodega(id_bodega);
CREATE INDEX idx_despacho_sucursal ON despachos_bodega(id_sucursal_destino);
CREATE INDEX idx_despacho_estado_fecha ON despachos_bodega(estado, fecha_registro);
