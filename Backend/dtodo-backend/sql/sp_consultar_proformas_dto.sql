CREATE PROCEDURE `sp_consultar_proformas_dto`(IN pFechaIni DATE, IN pFechaFin DATE)
BEGIN
	DECLARE vFechaIni DATE;
    DECLARE vFechaFin DATE;

    BEGIN
		SET vFechaIni = pFechaIni;
        SET vFechaFin = pFechaFin;

        IF vFechaIni IS NULL OR vFechaIni = '' THEN
            SELECT pro.id_proforma as idProforma,
                   pro.no_proforma as noProforma,
                   pro.fecha_emision as fechaEmision,
                   pro.total,
                   es.estado,
                   us.usuario,
                   cl.nombre as cliente
            FROM proformas as pro
            INNER JOIN estados as es ON es.id_estado = pro.id_estado
            INNER JOIN usuarios as us ON us.id_usuario = pro.id_usuario
            INNER JOIN clientes as cl ON cl.id_cliente = pro.id_cliente;
        ELSE
            SELECT pro.id_proforma as idProforma,
                   pro.no_proforma as noProforma,
                   pro.fecha_emision as fechaEmision,
                   pro.total,
                   es.estado,
                   us.usuario,
                   cl.nombre as cliente
            FROM proformas as pro
            INNER JOIN estados as es ON es.id_estado = pro.id_estado
            INNER JOIN usuarios as us ON us.id_usuario = pro.id_usuario
            INNER JOIN clientes as cl ON cl.id_cliente = pro.id_cliente
            WHERE pro.fecha_emision BETWEEN vFechaIni AND vFechaFin;
        END IF;
    END;
END