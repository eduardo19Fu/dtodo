-- El código de cliente pasa a ser un UUID (36 caracteres) que asigna el servidor al crear el cliente y no se edita.
-- Los clientes mayoristas que no tenían código reciben uno ahora; los minoristas lo reciben la próxima vez que se guarden.
-- Un código que ya existía (por ejemplo, un teléfono) se conserva tal cual.
ALTER TABLE clientes MODIFY COLUMN codigo_cliente VARCHAR(36) NULL;

UPDATE clientes SET codigo_cliente = UUID() WHERE tipo_cliente = 'MAYORISTA' AND codigo_cliente IS NULL;
