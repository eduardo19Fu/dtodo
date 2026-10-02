-- Revierte el módulo de Bodegas. DESTRUCTIVO: borra todos los datos de bodegas, despachos y movimientos.
-- Antes de correrlo hay que quitar el rol ROLE_BODEGA a los usuarios que lo tengan (usuarios_roles).
DROP TABLE IF EXISTS despachos_bodega_detalle;
DROP TABLE IF EXISTS despachos_bodega;
DROP TABLE IF EXISTS movimientos_bodega;
DROP TABLE IF EXISTS inventario_bodega;
DROP TABLE IF EXISTS bodegas;
DELETE FROM roles WHERE role = 'ROLE_BODEGA'
  AND NOT EXISTS (SELECT 1 FROM usuarios_roles ur WHERE ur.role_id = roles.id_role);
