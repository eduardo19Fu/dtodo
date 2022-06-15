CREATE FUNCTION `get_cant_clientes`() RETURNS int 
BEGIN 
	DECLARE cant_clientes INT;
	SELECT COUNT(*) INTO cant_clientes FROM clientes;
	return cant_clientes;
END 