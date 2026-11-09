-- Módulo de Clientes Mayoristas, Fase 3: historial de cambios de las metas de comisión.
-- Aditivo. Se aplica una sola vez, como los demás scripts de este módulo.
--
--   comisiones_metas_historial   Un renglón por cada cambio de las metas de un año: quién, cuándo, con qué motivo y cómo
--                                eran las metas antes y después. Las metas se guardan como texto «monto:porcentaje;monto:porcentaje».
--                                Sirve para reconstruir qué metas regían cuando un vendedor reclama su comisión.
CREATE TABLE comisiones_metas_historial (
    id_historial BIGINT AUTO_INCREMENT PRIMARY KEY,
    anio INT NOT NULL,
    accion VARCHAR(20) NOT NULL,
    id_usuario INT NOT NULL,
    motivo VARCHAR(500) NULL,
    antes VARCHAR(2000) NULL,
    despues VARCHAR(2000) NULL,
    fecha DATETIME NOT NULL,
    INDEX idx_comision_metas_historial_anio (anio, fecha),
    CONSTRAINT fk_comision_metas_historial_usuario FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario)
);
