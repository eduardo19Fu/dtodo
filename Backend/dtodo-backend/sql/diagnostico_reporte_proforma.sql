-- Diagnóstico de sólo lectura para el reporte PDF de proformas.
-- Sustituir el ID si se desea comprobar otra proforma.
SET @proforma_id = 29272;

SELECT DATABASE() AS base_datos, VERSION() AS version_mysql;

SELECT ROUTINE_SCHEMA, ROUTINE_NAME, ROUTINE_TYPE
FROM information_schema.ROUTINES
WHERE ROUTINE_SCHEMA = DATABASE()
  AND ROUTINE_NAME = 'get_numero_letras';

SELECT COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'proformas_detalle'
  AND COLUMN_NAME IN ('descuento', 'nuevo_precio_venta', 'sub_total_descuento');

SELECT pr.id_proforma,
       pr.no_proforma,
       COUNT(prd.id_detalle) AS lineas,
       get_numero_letras(pr.total) AS total_letras
FROM proformas AS pr
LEFT JOIN proformas_detalle AS prd ON prd.id_proforma = pr.id_proforma
WHERE pr.id_proforma = @proforma_id
GROUP BY pr.id_proforma, pr.no_proforma, pr.total;
