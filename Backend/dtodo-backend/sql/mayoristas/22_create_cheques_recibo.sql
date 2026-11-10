-- Módulo de Clientes Mayoristas: control de cheques.
-- Aditivo. Se aplica una sola vez, como los demás scripts de este módulo.
--
--   cheques_recibo   Los datos de cada cheque con que un cliente paga un recibo de caja (relación 1 a 1 con recibos_caja):
--                    banco, número, quién lo gira, fecha escrita en el cheque (si es posterior al pago, es postfechado),
--                    notas libres y el seguimiento: RECIBIDO, DEPOSITADO, COBRADO o RECHAZADO, con quién y cuándo cambió
--                    de estado y el motivo del rechazo. Un cheque rechazado anula su recibo (el saldo del cliente vuelve a subir).
CREATE TABLE cheques_recibo (
    id_cheque BIGINT AUTO_INCREMENT PRIMARY KEY,
    id_recibo BIGINT NOT NULL,
    banco VARCHAR(80) NOT NULL,
    numero_cheque VARCHAR(40) NOT NULL,
    girador VARCHAR(120) NULL,
    fecha_cheque DATE NOT NULL,
    notas VARCHAR(255) NULL,
    estado VARCHAR(15) NOT NULL DEFAULT 'RECIBIDO',
    fecha_estado DATETIME NOT NULL,
    id_usuario_estado INT NULL,
    motivo_rechazo VARCHAR(500) NULL,
    CONSTRAINT uq_cheque_recibo UNIQUE (id_recibo),
    CONSTRAINT fk_cheque_recibo FOREIGN KEY (id_recibo) REFERENCES recibos_caja(id_recibo),
    CONSTRAINT fk_cheque_usuario_estado FOREIGN KEY (id_usuario_estado) REFERENCES usuarios(id_usuario)
);

CREATE INDEX idx_cheque_estado ON cheques_recibo(estado, fecha_cheque);
CREATE INDEX idx_cheque_numero ON cheques_recibo(banco, numero_cheque);
