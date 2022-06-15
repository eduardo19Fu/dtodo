CREATE FUNCTION `get_cant_productos`() RETURNS int 
BEGIN 
	DECLARE cant_productos INT;
	SELECT
	    COUNT(*) INTO cant_productos
	FROM
	    productos
	WHERE
	    id_estado <> 5;
	return cant_productos;
END 