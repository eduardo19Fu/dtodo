package xyz.pangosoft.dtodo.service;

import xyz.pangosoft.dtodo.dto.IndicadoresMesDto;

/**
 * Indicadores del administrador. Ventas de un vendedor = lo entregado de sus pedidos mayoristas en el mes (por la fecha real de
 * entrega) menos lo que los clientes devolvieron en el mes. A tiempo = pedidos entregados por completo cuya última entrega fue en
 * o antes de la fecha estimada; los pedidos sin fecha estimada no cuentan. Cuentas por cobrar = la cobranza de hoy. Las reglas
 * responden 422 (401 y 403 los reserva el interceptor del frontend).
 */
public interface IIndicadoresService {

    IndicadoresMesDto indicadores(int anio, int mes);
}
