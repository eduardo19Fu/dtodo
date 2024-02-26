CREATE PROCEDURE `sp_consultar_productos`(IN idEstado INT)
BEGIN
    IF idEstado = 0 THEN
        SELECT *
        FROM productos
        ORDER BY nombre;
    ELSE
        SELECT *
        FROM productos
        WHERE id_estado = idEstado
        ORDER BY nombre;
    END IF;
END