CREATE FUNCTION `get_cant_usuarios`() RETURNS int 
BEGIN 
	DECLARE users INT;
	SELECT
	    COUNT(*) INTO users
	FROM
	    usuarios
	WHERE
	    enabled <> 0;
	return users;
END 