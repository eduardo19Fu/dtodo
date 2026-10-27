-- Módulo de Clientes Mayoristas, Fase 3: comisiones de los vendedores.
-- Aditivo. Se aplica una sola vez, como los demás scripts de este módulo.
--
--   comisiones_metas            Las metas de cada año: desde qué monto entregado se alcanza un nivel y qué porcentaje
--                               plano se paga sobre el total. Cada año tiene su propio juego, así que cambiar el esquema
--                               en un año nuevo no altera los años cerrados.
--   comisiones_liquidaciones    Lo que se liquidó por vendedor y año: monto entregado, meta alcanzada, porcentaje,
--                               comisión y si el vendedor completó el año (lo marca el administrador). Quien no completó
--                               el año queda con comisión cero. Un año liquidado no se recalcula.
--   comisiones_deshechas        Constancia de cada liquidación que el administrador deshizo, con motivo y un resumen de lo
--                               que se había liquidado.
CREATE TABLE comisiones_metas (
    id_meta INT AUTO_INCREMENT PRIMARY KEY,
    anio INT NOT NULL,
    monto_minimo DECIMAL(14,2) NOT NULL,
    porcentaje DECIMAL(5,2) NOT NULL,
    CONSTRAINT uq_comision_meta UNIQUE (anio, monto_minimo)
);

CREATE TABLE comisiones_liquidaciones (
    id_liquidacion BIGINT AUTO_INCREMENT PRIMARY KEY,
    anio INT NOT NULL,
    id_vendedor INT NOT NULL,
    monto_entregado DECIMAL(14,2) NOT NULL,
    monto_meta DECIMAL(14,2) NULL,
    porcentaje DECIMAL(5,2) NOT NULL DEFAULT 0,
    comision DECIMAL(14,2) NOT NULL DEFAULT 0,
    completo_anio TINYINT(1) NOT NULL,
    id_usuario INT NOT NULL,
    fecha DATETIME NOT NULL,
    CONSTRAINT uq_comision_liquidacion UNIQUE (anio, id_vendedor),
    CONSTRAINT fk_comision_liquidacion_vendedor FOREIGN KEY (id_vendedor) REFERENCES usuarios(id_usuario),
    CONSTRAINT fk_comision_liquidacion_usuario FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario)
);

CREATE TABLE comisiones_deshechas (
    id_deshecha BIGINT AUTO_INCREMENT PRIMARY KEY,
    anio INT NOT NULL,
    id_usuario INT NOT NULL,
    motivo VARCHAR(500) NOT NULL,
    resumen VARCHAR(4000) NULL,
    fecha DATETIME NOT NULL,
    CONSTRAINT fk_comision_deshecha_usuario FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario)
);

-- Las metas vigentes (Q300,000 → 0.5 %; Q700,000 → 1 %; Q1,000,000 → 1.5 %; Q2,000,000 → 2 %) para 2026.
INSERT INTO comisiones_metas (anio, monto_minimo, porcentaje) VALUES
    (2026, 300000.00, 0.50),
    (2026, 700000.00, 1.00),
    (2026, 1000000.00, 1.50),
    (2026, 2000000.00, 2.00);
