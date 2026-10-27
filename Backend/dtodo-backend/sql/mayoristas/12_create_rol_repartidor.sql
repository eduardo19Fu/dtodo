-- Módulo de Clientes Mayoristas, Fase 2C: rol Repartidor y piloto asignado al pedido.
-- Aditivo. Las columnas nuevas (ALTER TABLE) solo se aplican una vez, como en los demás scripts de este módulo.
--
--   ROLE_REPARTIDOR       Los pilotos que entregan. Ven solo los pedidos que se les asignaron y registran la entrega.
--                         roles.id_role NO es AUTO_INCREMENT en este esquema, por eso el id se calcula explícitamente
--                         (idempotente: no duplica el rol si el script se corre dos veces).
--   proformas.id_repartidor  Piloto asignado al cargar el pedido a ruta; lo puede cambiar el administrador o bodega.
INSERT INTO roles (id_role, role)
SELECT siguiente.id, 'ROLE_REPARTIDOR'
FROM (SELECT COALESCE(MAX(id_role), 0) + 1 AS id FROM roles) siguiente
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE role = 'ROLE_REPARTIDOR');

ALTER TABLE proformas
    ADD COLUMN id_repartidor INT NULL,
    ADD CONSTRAINT fk_proforma_repartidor FOREIGN KEY (id_repartidor) REFERENCES usuarios(id_usuario);

CREATE INDEX idx_proforma_repartidor ON proformas (id_repartidor);
