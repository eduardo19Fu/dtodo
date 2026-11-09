-- Quita los datos de prueba creados por seed_pedidos_rutas.sql (pedidos PRB....P, clientes PRB-.... y rutas de prueba).
-- No toca nada que no lleve esas marcas.
DELETE FROM proformas_etapas WHERE id_proforma IN (SELECT id_proforma FROM proformas WHERE no_proforma LIKE 'PRB%P');
DELETE FROM proformas WHERE no_proforma LIKE 'PRB%P';
DELETE FROM clientes WHERE nit LIKE 'PRB-%';
DELETE FROM rutas WHERE zona = 'DATOS DE PRUEBA';
