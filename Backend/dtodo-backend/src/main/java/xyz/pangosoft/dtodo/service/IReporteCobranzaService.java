package xyz.pangosoft.dtodo.service;

/** Los reportes de la cobranza: cuentas por cobrar por antigüedad (PDF o Excel) y estado de cuenta de un cliente (PDF). */
public interface IReporteCobranzaService {

    /** Cuentas por cobrar de hoy por cliente y antigüedad; ruta y solo-vencidos son filtros opcionales. */
    byte[] cuentasPorCobrar(Integer idRuta, boolean soloVencidos, String formato);

    /** Estado de cuenta imprimible de un cliente, con el saldo corrido. */
    byte[] estadoDeCuenta(Integer idCliente);
}
