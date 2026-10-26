-- Módulo de Clientes Mayoristas, Fase 2A: etapas del pedido mayorista.
-- Aditivo. El inventario no se toca en esta fase: la entrega que descuenta existencias llega en la 2B.
-- Las columnas nuevas (ALTER TABLE) solo se aplican una vez, como en los demás scripts de este módulo.
--
--   proformas.estado_pedido  NULL en una proforma minorista. En una mayorista: CONFIRMADA (al crearse),
--                            EN_PREPARACION (picking), EMPACADA, EN_RUTA, y desde la 2B ENTREGADA_PARCIAL o ENTREGADA.
--   proformas_etapas         Quién registró cada paso, cuándo y, si el personal que lo hizo no tiene usuario
--                            (rotan), su nombre en `responsable`. `motivo` se llena al retroceder un pedido o al
--                            reiniciarlo porque se editó la proforma. id_proforma no lleva llave foránea a propósito:
--                            el historial de una proforma anulada (borrada) debe conservarse.
ALTER TABLE proformas
    ADD COLUMN estado_pedido VARCHAR(20) NULL;

-- Las proformas mayoristas que ya existían se consideran confirmadas.
UPDATE proformas SET estado_pedido = 'CONFIRMADA' WHERE tipo_proforma = 'MAYORISTA' AND estado_pedido IS NULL;

CREATE INDEX idx_proforma_estado_pedido ON proformas (estado_pedido);

CREATE TABLE proformas_etapas (
    id_etapa BIGINT AUTO_INCREMENT PRIMARY KEY,
    id_proforma BIGINT NOT NULL,
    etapa VARCHAR(20) NOT NULL,
    id_usuario INT NOT NULL,
    responsable VARCHAR(150) NULL,
    motivo VARCHAR(500) NULL,
    fecha DATETIME NOT NULL,
    CONSTRAINT fk_proforma_etapa_usuario FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario)
);

CREATE INDEX idx_proforma_etapa ON proformas_etapas (id_proforma, fecha);
