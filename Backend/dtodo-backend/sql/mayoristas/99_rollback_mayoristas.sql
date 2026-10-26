-- Revierte el módulo de Clientes Mayoristas (pasos 01 a 05). Orden inverso a la creación.
-- ATENCIÓN: borra la información de rutas, los datos mayoristas de los clientes y los descuentos. Respaldar antes.
ALTER TABLE proformas_detalle
    DROP COLUMN precio_unitario,
    DROP COLUMN tipo_descuento,
    DROP COLUMN porcentaje_sugerido;
DROP TABLE IF EXISTS descuentos_producto;
DROP TABLE IF EXISTS precios_mayorista;
DROP TABLE IF EXISTS categorias_descuento_niveles;

DELETE FROM usuarios_roles WHERE role_id IN (SELECT id_role FROM roles WHERE role = 'ROLE_VENDEDOR');
DELETE FROM roles WHERE role = 'ROLE_VENDEDOR';

ALTER TABLE clientes DROP FOREIGN KEY fk_cliente_ruta;
DROP INDEX idx_cliente_ruta ON clientes;
DROP INDEX idx_cliente_tipo ON clientes;
ALTER TABLE clientes DROP INDEX uq_cliente_codigo;
ALTER TABLE clientes
    DROP COLUMN codigo_cliente,
    DROP COLUMN nombre_negocio,
    DROP COLUMN tipo_cliente,
    DROP COLUMN categoria_descuento,
    DROP COLUMN id_ruta,
    DROP COLUMN contacto,
    DROP COLUMN telefono_contacto,
    DROP COLUMN referencia_ubicacion,
    DROP COLUMN limite_credito,
    DROP COLUMN dias_credito;

DROP TABLE IF EXISTS rutas;
