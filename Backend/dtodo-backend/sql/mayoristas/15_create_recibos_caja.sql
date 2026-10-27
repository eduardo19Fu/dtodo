-- Módulo de Clientes Mayoristas, Fase 4 (paso 1): recibos de caja.
-- Aditivo. Se aplica una sola vez, como los demás scripts de este módulo.
--
--   recibos_caja          Cada abono o depósito de un cliente: sucursal y número propio de la sucursal, cliente, el pedido
--                         (proforma) o la factura a la que paga (opcionales), monto, forma de pago, referencia, quién lo
--                         emitió y, si se anuló, quién, cuándo y por qué. Un recibo anulado deja de contar para el saldo.
--   recibos_correlativos  Último número emitido por sucursal. Se bloquea la fila al emitir para que dos recibos
--                         simultáneos no repitan número.
CREATE TABLE recibos_caja (
    id_recibo BIGINT AUTO_INCREMENT PRIMARY KEY,
    id_sucursal INT NOT NULL,
    numero BIGINT NOT NULL,
    id_cliente INT NOT NULL,
    id_proforma BIGINT NULL,
    id_factura BIGINT NULL,
    monto DECIMAL(12,2) NOT NULL,
    forma_pago VARCHAR(20) NOT NULL,
    referencia VARCHAR(100) NULL,
    observaciones VARCHAR(255) NULL,
    fecha_pago DATE NOT NULL,
    fecha_registro DATETIME NOT NULL,
    id_usuario INT NOT NULL,
    anulado TINYINT(1) NOT NULL DEFAULT 0,
    fecha_anulacion DATETIME NULL,
    id_usuario_anulacion INT NULL,
    motivo_anulacion VARCHAR(500) NULL,
    CONSTRAINT uq_recibo_numero UNIQUE (id_sucursal, numero),
    CONSTRAINT fk_recibo_sucursal FOREIGN KEY (id_sucursal) REFERENCES sucursales(id_sucursal),
    CONSTRAINT fk_recibo_cliente FOREIGN KEY (id_cliente) REFERENCES clientes(id_cliente),
    CONSTRAINT fk_recibo_proforma FOREIGN KEY (id_proforma) REFERENCES proformas(id_proforma),
    CONSTRAINT fk_recibo_factura FOREIGN KEY (id_factura) REFERENCES facturas(id_factura),
    CONSTRAINT fk_recibo_usuario FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario),
    CONSTRAINT fk_recibo_usuario_anula FOREIGN KEY (id_usuario_anulacion) REFERENCES usuarios(id_usuario)
);

CREATE INDEX idx_recibo_cliente ON recibos_caja(id_cliente, anulado);
CREATE INDEX idx_recibo_proforma ON recibos_caja(id_proforma);
CREATE INDEX idx_recibo_fecha ON recibos_caja(fecha_pago);

CREATE TABLE recibos_correlativos (
    id_sucursal INT PRIMARY KEY,
    ultimo BIGINT NOT NULL,
    CONSTRAINT fk_recibo_correlativo_sucursal FOREIGN KEY (id_sucursal) REFERENCES sucursales(id_sucursal)
);

INSERT INTO recibos_correlativos (id_sucursal, ultimo) SELECT id_sucursal, 0 FROM sucursales;
