-- Revierte el módulo de Clientes Mayoristas (pasos 01 a 16). Orden inverso a la creación.
-- ATENCIÓN: borra la información de rutas, los datos mayoristas de los clientes y los descuentos. Respaldar antes.
-- Paso 16
ALTER TABLE proformas DROP COLUMN condicion_pago, DROP COLUMN dias_credito;

-- Paso 15
DROP TABLE IF EXISTS recibos_correlativos;
DROP TABLE IF EXISTS recibos_caja;

-- Paso 14: los codigos UUID no caben en 30 caracteres; se borran antes de reducir la columna.
UPDATE clientes SET codigo_cliente = NULL WHERE CHAR_LENGTH(codigo_cliente) > 30;
ALTER TABLE clientes MODIFY COLUMN codigo_cliente VARCHAR(30) NULL;

-- Paso 13
DROP TABLE IF EXISTS comisiones_deshechas;
DROP TABLE IF EXISTS comisiones_liquidaciones;
DROP TABLE IF EXISTS comisiones_metas;

-- Paso 12
ALTER TABLE proformas DROP FOREIGN KEY fk_proforma_repartidor;
DROP INDEX idx_proforma_repartidor ON proformas;
ALTER TABLE proformas DROP COLUMN id_repartidor;
DELETE FROM usuarios_roles WHERE role_id IN (SELECT id_role FROM roles WHERE role = 'ROLE_REPARTIDOR');
DELETE FROM roles WHERE role = 'ROLE_REPARTIDOR';

ALTER TABLE facturas_detalle DROP COLUMN cantidad_descontada;

-- Paso 10: los valores nuevos del ENUM de movimientos_producto (ENTREGA_PROFORMA...) se dejan: quitarlos fallaria si ya
-- hay movimientos con ese tipo.
DROP TABLE IF EXISTS proformas_entregas_detalle;
DROP TABLE IF EXISTS proformas_entregas;
ALTER TABLE proformas_detalle
    DROP COLUMN cantidad_entregada,
    DROP COLUMN cantidad_descontada;

DROP TABLE IF EXISTS proformas_etapas;
DROP INDEX idx_proforma_estado_pedido ON proformas;
ALTER TABLE proformas DROP COLUMN estado_pedido;

-- Paso 08: falla si ya hay proformas con dos facturas (una anulada y su reemisión); anule o reasigne esas facturas antes.
CREATE UNIQUE INDEX uq_facturas_proforma_origen ON facturas (id_proforma_origen);
DROP INDEX idx_facturas_proforma_origen ON facturas;

DROP TABLE IF EXISTS proformas_auditoria;
DROP INDEX idx_proforma_autorizacion ON proformas;
ALTER TABLE proformas
    DROP COLUMN estado_autorizacion,
    DROP COLUMN vencimiento_autorizado;
ALTER TABLE proformas_detalle DROP COLUMN observacion;
ALTER TABLE proformas DROP FOREIGN KEY fk_proforma_ruta;
DROP INDEX idx_proforma_tipo ON proformas;
ALTER TABLE proformas
    DROP COLUMN tipo_proforma,
    DROP COLUMN id_ruta,
    DROP COLUMN observaciones,
    DROP COLUMN fecha_vencimiento,
    DROP COLUMN fecha_entrega_estimada;
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
