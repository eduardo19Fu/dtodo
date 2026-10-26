-- Módulo de Clientes Mayoristas: auditoría de proformas y autorizaciones.
-- Aditivo. Las proformas existentes quedan sin estado de autorización (no la necesitan) y sin vencimiento autorizado.
-- Las columnas nuevas (ALTER TABLE) solo se aplican una vez, como en los demás scripts de este módulo.
--
--   estado_autorizacion     NULL (no requiere), PENDIENTE, APROBADA o RECHAZADA. Una proforma mayorista con una línea
--                           sobre el tope de descuento queda PENDIENTE y no se factura hasta que un administrador la apruebe.
--   vencimiento_autorizado  1 cuando un administrador autorizó facturar la proforma pese a estar vencida.
--   proformas_auditoria     Quién hizo qué sobre cada proforma: creación, modificación, anulación, autorización,
--                           rechazo y cambio de cliente al facturar. id_proforma no lleva llave foránea a propósito:
--                           el historial de una proforma anulada (borrada) debe conservarse.
ALTER TABLE proformas
    ADD COLUMN estado_autorizacion VARCHAR(20) NULL,
    ADD COLUMN vencimiento_autorizado TINYINT(1) NOT NULL DEFAULT 0;

CREATE INDEX idx_proforma_autorizacion ON proformas (estado_autorizacion);

CREATE TABLE proformas_auditoria (
    id_auditoria BIGINT AUTO_INCREMENT PRIMARY KEY,
    id_proforma BIGINT NOT NULL,
    id_usuario INT NOT NULL,
    accion VARCHAR(30) NOT NULL,
    motivo VARCHAR(500) NULL,
    detalle TEXT NULL,
    fecha DATETIME NOT NULL,
    CONSTRAINT fk_proforma_auditoria_usuario FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario)
);

CREATE INDEX idx_proforma_auditoria ON proformas_auditoria (id_proforma, fecha);
