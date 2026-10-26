-- DIAGNÓSTICO (solo lectura): clientes cuyo NIT no parece un NIT.
-- Hay clientes con el teléfono guardado en la columna nit; la SAT los rechaza al certificar la factura y
-- la venta se revierte. El sistema valida el dígito verificador (módulo 11) antes de facturar; esta consulta
-- sirve para dimensionar el problema en cada ambiente ANTES de depurar los datos. No modifica nada.
--
-- La pantalla de Clientes ofrece el filtro "NIT inválido" (solo administrador), que aplica el cálculo exacto
-- del dígito verificador; aquí solo se detecta lo que se puede ver sin ese cálculo.

-- 1) Resumen: cuántos clientes tienen cada clase de valor en el NIT.
SELECT
    CASE
        WHEN nit IS NULL OR TRIM(nit) = '' THEN 'Sin NIT'
        WHEN UPPER(REPLACE(REPLACE(TRIM(nit), '-', ''), '/', '')) = 'CF' THEN 'Consumidor final (CF)'
        WHEN REPLACE(TRIM(nit), '-', '') NOT REGEXP '^[0-9]{1,11}[0-9Kk]$' THEN 'Formato inválido'
        WHEN telefono IS NOT NULL AND REPLACE(REPLACE(TRIM(telefono), '-', ''), ' ', '') = REPLACE(TRIM(nit), '-', '')
            THEN 'Igual al teléfono'
        ELSE 'Con formato de NIT (verificar dígito en el sistema)'
    END AS clase,
    COUNT(*) AS clientes
FROM clientes
GROUP BY clase
ORDER BY clientes DESC;

-- 2) Detalle: el NIT es idéntico al teléfono o no tiene formato de NIT. Son los candidatos a corregir.
SELECT id_cliente, nombre, nit, telefono, tipo_cliente
FROM clientes
WHERE nit IS NOT NULL
  AND TRIM(nit) <> ''
  AND UPPER(REPLACE(REPLACE(TRIM(nit), '-', ''), '/', '')) <> 'CF'
  AND (
        REPLACE(TRIM(nit), '-', '') NOT REGEXP '^[0-9]{1,11}[0-9Kk]$'
        OR (telefono IS NOT NULL AND REPLACE(REPLACE(TRIM(telefono), '-', ''), ' ', '') = REPLACE(TRIM(nit), '-', ''))
      )
ORDER BY nombre;

-- 3) Facturas ya emitidas a clientes con ese NIT: no se tocan (guardan el NIT con el que se certificaron),
--    pero conviene saber si el problema ya afectó ventas.
SELECT c.id_cliente, c.nombre, c.nit, COUNT(f.id_factura) AS facturas
FROM clientes c
JOIN facturas f ON f.id_cliente = c.id_cliente
WHERE c.nit IS NOT NULL
  AND TRIM(c.nit) <> ''
  AND UPPER(REPLACE(REPLACE(TRIM(c.nit), '-', ''), '/', '')) <> 'CF'
  AND REPLACE(TRIM(c.nit), '-', '') NOT REGEXP '^[0-9]{1,11}[0-9Kk]$'
GROUP BY c.id_cliente, c.nombre, c.nit
ORDER BY facturas DESC;
