-- =============================================================================
-- Script correctivo: alinear PRODUCCION (prstd_db) con el modulo Notas de Credito
-- Objetivo: llevar el esquema de prstd_db al mismo estado que prstd_db_testing
--           para que el backend (rama proyecto-refactorizacion) funcione.
-- Fecha: 2026-07-18
-- IMPORTANTE: Hacer BACKUP antes de ejecutar. Revisar seccion de datos legacy.
-- =============================================================================

USE prstd_db;

-- -----------------------------------------------------------------------------
-- 1) Tabla notas_credito: migrar de esquema legacy al esquema de la entidad
-- -----------------------------------------------------------------------------

-- 1.1 Nuevas columnas requeridas por la entidad NotaCredito.java
ALTER TABLE notas_credito
    ADD COLUMN IF NOT EXISTS correlativo_factura_sat VARCHAR(25) NULL AFTER id_nota_credito,
    ADD COLUMN IF NOT EXISTS serie_factura_sat       VARCHAR(200) NULL AFTER correlativo_factura_sat,
    ADD COLUMN IF NOT EXISTS tipo_documento_origen   VARCHAR(20) NOT NULL DEFAULT 'FACTURA' AFTER serie_factura_sat,
    ADD COLUMN IF NOT EXISTS no_proforma             VARCHAR(50) NULL AFTER tipo_documento_origen,
    ADD COLUMN IF NOT EXISTS fecha_entrega_estimada  DATE NULL AFTER fecha_creacion,
    ADD COLUMN IF NOT EXISTS observaciones           VARCHAR(500) NULL,
    ADD COLUMN IF NOT EXISTS estado                  VARCHAR(50) NOT NULL DEFAULT 'ENTREGA_PENDIENTE';

-- 1.2 Backfill defensivo de registros legacy
UPDATE notas_credito SET tipo_documento_origen = 'FACTURA' WHERE tipo_documento_origen IS NULL;
UPDATE notas_credito SET estado = 'ENTREGA_PENDIENTE' WHERE estado IS NULL OR estado = '';

-- 1.3 Eliminar columnas/relaciones legacy que la entidad ya NO usa
--     (abono, restante, fecha_pago_limite, id_estado)
ALTER TABLE notas_credito DROP FOREIGN KEY IF EXISTS fk_notas_credito_idestado;
ALTER TABLE notas_credito DROP COLUMN IF EXISTS id_estado;
ALTER TABLE notas_credito DROP COLUMN IF EXISTS abono;
ALTER TABLE notas_credito DROP COLUMN IF EXISTS restante;
ALTER TABLE notas_credito DROP COLUMN IF EXISTS fecha_pago_limite;

-- 1.4 Indices de soporte para validaciones de duplicados
CREATE INDEX IF NOT EXISTS idx_notas_credito_tipo_origen ON notas_credito (tipo_documento_origen);
CREATE INDEX IF NOT EXISTS idx_notas_credito_no_proforma ON notas_credito (no_proforma);

-- -----------------------------------------------------------------------------
-- 2) Tabla despachos_nota: NO existe en produccion, crearla igual que en dev
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS despachos_nota (
    id_despacho     BIGINT NOT NULL AUTO_INCREMENT,
    id_evento       VARCHAR(36) NOT NULL,
    id_nota_credito INT NOT NULL,
    id_producto     INT NOT NULL,
    id_usuario      INT NOT NULL,
    cod_producto    VARCHAR(100) NOT NULL,
    cantidad        INT NOT NULL,
    total_despacho  DECIMAL(10,2) NOT NULL,
    fecha_despacho  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id_despacho),
    KEY fk_despacho_nota_credito (id_nota_credito),
    KEY fk_despacho_producto (id_producto),
    KEY fk_despacho_usuario_idx (id_usuario),
    KEY idx_despacho_evento (id_evento),
    CONSTRAINT fk_despacho_nota_credito FOREIGN KEY (id_nota_credito) REFERENCES notas_credito (id_nota_credito) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_despacho_producto     FOREIGN KEY (id_producto)     REFERENCES productos (id_producto)         ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_despacho_usuario      FOREIGN KEY (id_usuario)      REFERENCES usuarios (id_usuario)           ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

-- -----------------------------------------------------------------------------
-- 3) Verificacion post-migracion (ejecutar y comparar con dev)
-- -----------------------------------------------------------------------------
-- SHOW CREATE TABLE notas_credito\G
-- SHOW CREATE TABLE despachos_nota\G
-- pagos_parciales y notas_credito_detalle YA coinciden con dev (no requieren cambios).
