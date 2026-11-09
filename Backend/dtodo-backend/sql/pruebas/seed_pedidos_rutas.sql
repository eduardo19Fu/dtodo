-- Datos de PRUEBA para ver el listado de pedidos con varias rutas, la nota de envío y la hoja de picking.
-- Solo para la base de pruebas (prstd_db_testing). Es repetible: borra lo que creó antes y lo vuelve a crear.
-- Identificación de lo que crea, para poder quitarlo con unseed_pedidos_rutas.sql:
--   * pedidos con no_proforma 'PRB....P'
--   * clientes con NIT 'PRB-....'
--   * rutas con zona 'DATOS DE PRUEBA'
-- No toca los pedidos ni las rutas que ya existían.

SET @sucursal := 1;
SET @vendedor := 1;     -- usuario que figura como vendedor (efuentes)
SET @piloto := 32;      -- repartidor.prueba

-- ---------------------------------------------------------------- limpieza de una carga anterior
DELETE FROM proformas_etapas WHERE id_proforma IN (SELECT id_proforma FROM proformas WHERE no_proforma LIKE 'PRB%P');
DELETE FROM proformas WHERE no_proforma LIKE 'PRB%P';
DELETE FROM clientes WHERE nit LIKE 'PRB-%';
DELETE FROM rutas WHERE zona = 'DATOS DE PRUEBA';

-- ---------------------------------------------------------------- rutas
INSERT INTO rutas (nombre, zona, dias_visita, fecha_registro, id_vendedor, id_sucursal, id_estado) VALUES
  ('RUTA ORIENTE',    'DATOS DE PRUEBA', 'Lunes y jueves',   NOW(), NULL, @sucursal, 1),
  ('RUTA SUR',        'DATOS DE PRUEBA', 'Martes y viernes', NOW(), NULL, @sucursal, 1),
  ('RUTA OCCIDENTE',  'DATOS DE PRUEBA', 'Miércoles',        NOW(), NULL, @sucursal, 1);

-- ---------------------------------------------------------------- clientes mayoristas, uno por ruta
INSERT INTO clientes (nombre, nit, direccion, fecha_registro, telefono, codigo_cliente, nombre_negocio, tipo_cliente,
                      categoria_descuento, id_ruta, contacto, telefono_contacto, limite_credito, dias_credito)
SELECT 'PRUEBA Papeleria El Oriente', 'PRB-0001', 'Barrio El Calvario, Sanarate', NOW(), '7922-0001', UUID(),
       'Papeleria El Oriente', 'MAYORISTA', 'A', r.id_ruta, 'Marta Lopez', '5511-2001', NULL, NULL
  FROM rutas r WHERE r.nombre = 'RUTA ORIENTE' AND r.zona = 'DATOS DE PRUEBA'
UNION ALL
SELECT 'PRUEBA Distribuidora Sur', 'PRB-0002', '2a calle 4-10, zona 1, Monjas', NOW(), '7922-0002', UUID(),
       'Distribuidora del Sur', 'MAYORISTA', 'A', r.id_ruta, 'Carlos Ruiz', '5522-3002', NULL, NULL
  FROM rutas r WHERE r.nombre = 'RUTA SUR' AND r.zona = 'DATOS DE PRUEBA'
UNION ALL
SELECT 'PRUEBA Libreria Occidente', 'PRB-0003', 'Centro, Mataquescuintla', NOW(), NULL, UUID(),
       'Libreria Occidente', 'MAYORISTA', 'A', r.id_ruta, NULL, NULL, NULL, NULL
  FROM rutas r WHERE r.nombre = 'RUTA OCCIDENTE' AND r.zona = 'DATOS DE PRUEBA';

-- ---------------------------------------------------------------- pedidos
CREATE TEMPORARY TABLE tmp_pedidos (
  no_proforma VARCHAR(20), cliente VARCHAR(300), estado VARCHAR(20), entrega_dias INT NULL, piloto INT NULL, observaciones VARCHAR(500) NULL
);
INSERT INTO tmp_pedidos VALUES
  ('PRB1001P', 'CLIENTE PRUEBA MAYORISTA',       'EMPACADA',       1,    NULL,   'Pedido de prueba: por cargar en RUTA PRUEBA'),
  ('PRB2001P', 'PRUEBA Papeleria El Oriente',    'EMPACADA',       1,    NULL,   'Entregar por la mañana'),
  ('PRB2002P', 'PRUEBA Papeleria El Oriente',    'EN_RUTA',        0,    @piloto, NULL),
  ('PRB3001P', 'PRUEBA Distribuidora Sur',       'CONFIRMADA',     3,    NULL,   NULL),
  ('PRB3002P', 'PRUEBA Distribuidora Sur',       'EN_PREPARACION', 2,    NULL,   'Empacar en bolsa individual por niño'),
  ('PRB4001P', 'PRUEBA Libreria Occidente',      'EMPACADA',       NULL, NULL,   NULL);

INSERT INTO proformas (no_proforma, total, fecha_emision, id_usuario, id_estado, id_cliente, id_sucursal, tipo_proforma, id_ruta,
                       observaciones, fecha_vencimiento, fecha_entrega_estimada, estado_autorizacion, vencimiento_autorizado,
                       estado_pedido, id_repartidor, condicion_pago, dias_credito)
SELECT t.no_proforma, 0, NOW(), @vendedor, 1, c.id_cliente, @sucursal, 'MAYORISTA', c.id_ruta,
       t.observaciones, DATE_ADD(CURDATE(), INTERVAL 15 DAY),
       CASE WHEN t.entrega_dias IS NULL THEN NULL ELSE DATE_ADD(CURDATE(), INTERVAL t.entrega_dias DAY) END,
       NULL, 0, t.estado, t.piloto, 'CONTADO', NULL
  FROM tmp_pedidos t
  INNER JOIN clientes c ON c.nombre = t.cliente;

-- ---------------------------------------------------------------- líneas (producto, cantidad, observación)
CREATE TEMPORARY TABLE tmp_lineas (no_proforma VARCHAR(20), id_producto INT, cantidad INT, obs VARCHAR(255) NULL);
INSERT INTO tmp_lineas VALUES
  ('PRB1001P', 8, 12, NULL), ('PRB1001P', 9, 6, 'Forro azul'), ('PRB1001P', 12, 8, NULL),
  ('PRB2001P', 8, 20, NULL), ('PRB2001P', 11, 10, 'Edición especial'),
  ('PRB2002P', 9, 15, NULL), ('PRB2002P', 12, 5, NULL), ('PRB2002P', 2447, 30, 'Colores surtidos'),
  ('PRB3001P', 11, 18, NULL), ('PRB3001P', 8, 4, NULL),
  ('PRB3002P', 12, 25, NULL), ('PRB3002P', 9, 9, '10 rojos, 6 azules y 2 negros'),
  ('PRB4001P', 2447, 40, NULL), ('PRB4001P', 8, 7, NULL), ('PRB4001P', 11, 3, 'Revisar los empastes');

INSERT INTO proformas_detalle (id_producto, id_proforma, cantidad, descuento, nuevo_precio_venta, sub_total, sub_total_descuento,
                               precio_unitario, tipo_descuento, observacion, cantidad_entregada, cantidad_descontada, cantidad_devuelta)
SELECT l.id_producto, pr.id_proforma, l.cantidad, 0, p.precio_venta, l.cantidad * p.precio_venta, l.cantidad * p.precio_venta,
       p.precio_venta, 'MANUAL', l.obs, 0, 0, 0
  FROM tmp_lineas l
  INNER JOIN proformas pr ON pr.no_proforma = l.no_proforma
  INNER JOIN productos p ON p.id_producto = l.id_producto;

UPDATE proformas pr
   SET pr.total = (SELECT COALESCE(SUM(d.sub_total_descuento), 0) FROM proformas_detalle d WHERE d.id_proforma = pr.id_proforma)
 WHERE pr.no_proforma LIKE 'PRB%P';

-- ---------------------------------------------------------------- historial de etapas hasta la etapa actual
CREATE TEMPORARY TABLE tmp_etapas (etapa VARCHAR(20), orden INT, horas_atras INT);
INSERT INTO tmp_etapas VALUES ('CONFIRMADA', 1, 6), ('EN_PREPARACION', 2, 4), ('EMPACADA', 3, 2), ('EN_RUTA', 4, 0);

INSERT INTO proformas_etapas (id_proforma, etapa, id_usuario, responsable, motivo, fecha)
SELECT pr.id_proforma, e.etapa, @vendedor, NULL,
       CASE WHEN e.etapa = 'EN_RUTA' THEN 'Piloto: Repartidor Prueba' ELSE NULL END,
       DATE_SUB(NOW(), INTERVAL e.horas_atras HOUR)
  FROM proformas pr
  INNER JOIN tmp_etapas e ON e.orden <= CASE pr.estado_pedido
        WHEN 'CONFIRMADA' THEN 1 WHEN 'EN_PREPARACION' THEN 2 WHEN 'EMPACADA' THEN 3 WHEN 'EN_RUTA' THEN 4 END
 WHERE pr.no_proforma LIKE 'PRB%P';

-- ---------------------------------------------------------------- resumen de lo creado
SELECT r.nombre AS ruta, pr.no_proforma, pr.estado_pedido, pr.fecha_entrega_estimada AS entrega,
       (SELECT SUM(d.cantidad) FROM proformas_detalle d WHERE d.id_proforma = pr.id_proforma) AS unidades, pr.total
  FROM proformas pr INNER JOIN rutas r ON r.id_ruta = pr.id_ruta
 WHERE pr.no_proforma LIKE 'PRB%P'
 ORDER BY r.nombre, pr.no_proforma;
