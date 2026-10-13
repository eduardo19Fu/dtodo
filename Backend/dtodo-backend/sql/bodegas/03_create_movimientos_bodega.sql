-- Bitácora de movimientos de cada bodega. tipo_movimiento es VARCHAR (no ENUM) para poder sumar tipos
-- sin tener que alterar la tabla; los valores válidos los controla TipoMovimientoBodegaEnum.
-- tipo_movimiento: INGRESO, REDUCCION, ELIMINACION, DESPACHO, ANULACION_DESPACHO, IMPORTACION.
CREATE TABLE movimientos_bodega (
    id_movimiento BIGINT AUTO_INCREMENT PRIMARY KEY,
    fecha_movimiento DATETIME NOT NULL,
    tipo_movimiento VARCHAR(30) NOT NULL,
    cantidad INT NOT NULL,
    stock_inicial INT NOT NULL,
    stock_final INT NOT NULL,
    motivo VARCHAR(300),
    tipo_documento_origen VARCHAR(30),
    id_documento_origen BIGINT,
    id_bodega INT NOT NULL,
    id_producto INT NOT NULL,
    id_usuario INT NULL,
    CONSTRAINT fk_mov_bodega_bodega FOREIGN KEY (id_bodega) REFERENCES bodegas(id_bodega),
    CONSTRAINT fk_mov_bodega_producto FOREIGN KEY (id_producto) REFERENCES productos(id_producto),
    CONSTRAINT fk_mov_bodega_usuario FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario)
);

CREATE INDEX idx_mov_bodega_bodega_fecha ON movimientos_bodega(id_bodega, fecha_movimiento);
CREATE INDEX idx_mov_bodega_producto ON movimientos_bodega(id_producto);
CREATE INDEX idx_mov_bodega_documento ON movimientos_bodega(tipo_documento_origen, id_documento_origen);
