-- =============================================================================
-- Migración: Soporte de origen de Nota de Crédito (Factura | Proforma)
-- Fecha: 2026-05-10
-- Descripción:
--   Agrega columnas para discriminar el documento origen de una Nota de Crédito
--   (FACTURA o PROFORMA) y para almacenar el número de proforma cuando aplique.
-- =============================================================================

-- 1. Agregar columnas nuevas (no destructivo, default = FACTURA para legacy)
ALTER TABLE notas_credito
    ADD COLUMN IF NOT EXISTS tipo_documento_origen VARCHAR(20) NOT NULL DEFAULT 'FACTURA' AFTER serie_factura_sat,
    ADD COLUMN IF NOT EXISTS no_proforma VARCHAR(50) NULL AFTER tipo_documento_origen;

-- 2. Backfill defensivo: registros previos siempre quedan como FACTURA
UPDATE notas_credito
SET tipo_documento_origen = 'FACTURA'
WHERE tipo_documento_origen IS NULL;

-- 3. Índices de soporte para queries de validación
CREATE INDEX IF NOT EXISTS idx_notas_credito_tipo_origen
    ON notas_credito (tipo_documento_origen);

CREATE INDEX IF NOT EXISTS idx_notas_credito_no_proforma
    ON notas_credito (no_proforma);

-- =============================================================================
-- NOTA IMPORTANTE - Defensa en profundidad (índices únicos):
-- =============================================================================
-- Se evaluó añadir UNIQUE INDEX para garantizar a nivel de BD que no existan
-- Notas de Crédito duplicadas por documento origen. Sin embargo, se detectaron
-- duplicados pre-existentes en el ambiente de pruebas (prstd_db_testing) que
-- impedirían la creación de los índices.
--
-- La validación queda implementada a nivel de servicio (capa de negocio):
--   - NotaCreditoServiceImpl.save(...) lanza DuplicateNotaCreditoException
--     antes de persistir si ya existe una nota para el mismo documento origen.
--
-- Si en el futuro se desea reforzar con UNIQUE INDEX, primero ejecutar:
--   SELECT correlativo_factura_sat, serie_factura_sat, COUNT(*) AS total
--   FROM notas_credito
--   WHERE tipo_documento_origen = 'FACTURA'
--   GROUP BY correlativo_factura_sat, serie_factura_sat
--   HAVING COUNT(*) > 1;
--
-- Luego depurar los duplicados y ejecutar:
--   CREATE UNIQUE INDEX uq_notas_credito_factura
--       ON notas_credito (correlativo_factura_sat, serie_factura_sat, tipo_documento_origen);
--   CREATE UNIQUE INDEX uq_notas_credito_proforma
--       ON notas_credito (no_proforma);
-- =============================================================================
