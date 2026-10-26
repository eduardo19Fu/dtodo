-- Rutas iniciales informadas por el cliente (cuestionario 2, 16/10/2026). Idempotente.
-- Quedan SIN vendedor: se asignan desde la pantalla de Rutas cuando existan los usuarios con ROLE_VENDEDOR.
-- Todas en la sucursal principal; el estado se toma por nombre para no depender de ids.
INSERT INTO rutas (nombre, zona, dias_visita, fecha_registro, id_vendedor, id_sucursal, id_estado)
SELECT datos.nombre, datos.zona, datos.dias_visita, NOW(), NULL, principal.id_sucursal, activo.id_estado
FROM (
    SELECT 'RUTA 1' AS nombre, 'Monjas, San Manuel Chaparrón, San Carlos Alzatate' AS zona, 'Lunes cada 15 días' AS dias_visita
    UNION ALL SELECT 'RUTA 2', 'Santa María Xalapán, Mataquescuintla', 'Martes cada 15 días'
    UNION ALL SELECT 'RUTA 3', 'San Pedro Pinula', 'Lunes cada 15 días (alterna con la Ruta 1)'
    UNION ALL SELECT 'JALAPA', 'Jalapa, colonias y aldeas', 'Lunes y martes'
) datos
JOIN (SELECT id_sucursal FROM sucursales WHERE es_principal = 1 ORDER BY id_sucursal LIMIT 1) principal
JOIN (SELECT id_estado FROM estados WHERE estado = 'ACTIVO' LIMIT 1) activo
WHERE NOT EXISTS (
    SELECT 1 FROM rutas r WHERE r.nombre = datos.nombre AND r.id_sucursal = principal.id_sucursal
);
