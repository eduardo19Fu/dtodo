CREATE TABLE IF NOT EXISTS despachos_nota (
    id_despacho BIGINT AUTO_INCREMENT PRIMARY KEY,
    id_evento VARCHAR(36) NOT NULL,
    id_nota_credito BIGINT NOT NULL,
    id_producto INT NOT NULL,
    id_usuario INT NOT NULL,
    cod_producto VARCHAR(100) NOT NULL,
    cantidad INT NOT NULL,
    total_despacho DECIMAL(10,2) NOT NULL,
    fecha_despacho DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_despacho_nota_credito FOREIGN KEY (id_nota_credito) REFERENCES notas_credito(id_nota_credito) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_despacho_producto FOREIGN KEY (id_producto) REFERENCES productos(id_producto) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_despacho_usuario FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- Migracion para tablas existentes
ALTER TABLE despachos_nota
    ADD COLUMN IF NOT EXISTS id_evento VARCHAR(36) NULL AFTER id_despacho,
    ADD COLUMN IF NOT EXISTS id_usuario INT NULL AFTER id_producto;

ALTER TABLE despachos_nota
    ADD CONSTRAINT IF NOT EXISTS fk_despacho_usuario FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario);

CREATE INDEX IF NOT EXISTS idx_despacho_evento ON despachos_nota(id_evento);
