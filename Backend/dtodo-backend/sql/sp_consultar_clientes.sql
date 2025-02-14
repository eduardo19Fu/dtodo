CREATE PROCEDURE sp_consultar_clientes ()
BEGIN
    SELECT id_cliente,
           nombre,
           nit,
           direccion,
           fecha_registro,
           telefono
    FROM clientes;
END
