-- Módulo de Clientes Mayoristas: un solo recibo para varios pedidos.
-- Aditivo. Se aplica una sola vez, como los demás scripts de este módulo.
--
--   recibos_caja_pedidos   Cuánto de un recibo se aplica a cada pedido, cuando el cliente paga varios pedidos (por ejemplo, los de un
--                          mismo día y ruta) con un solo recibo. La suma de las partidas es el monto del recibo. Un recibo con partidas
--                          no tiene id_proforma: el cobro de cada pedido sale de sus partidas. Si el recibo se anula, sus partidas dejan
--                          de contar con él.
CREATE TABLE recibos_caja_pedidos (
    id_partida BIGINT AUTO_INCREMENT PRIMARY KEY,
    id_recibo BIGINT NOT NULL,
    id_proforma BIGINT NOT NULL,
    monto DECIMAL(12,2) NOT NULL,
    CONSTRAINT uq_recibo_pedido UNIQUE (id_recibo, id_proforma),
    CONSTRAINT fk_partida_recibo FOREIGN KEY (id_recibo) REFERENCES recibos_caja(id_recibo),
    CONSTRAINT fk_partida_proforma FOREIGN KEY (id_proforma) REFERENCES proformas(id_proforma)
);

CREATE INDEX idx_partida_proforma ON recibos_caja_pedidos(id_proforma);
