-- Nuevo rol para el módulo de Bodegas. Idempotente: no duplica el rol si el script se corre dos veces.
-- roles.id_role NO es AUTO_INCREMENT en este esquema, por eso el id se calcula explícitamente.
INSERT INTO roles (id_role, role)
SELECT siguiente.id, 'ROLE_BODEGA'
FROM (SELECT COALESCE(MAX(id_role), 0) + 1 AS id FROM roles) siguiente
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE role = 'ROLE_BODEGA');
