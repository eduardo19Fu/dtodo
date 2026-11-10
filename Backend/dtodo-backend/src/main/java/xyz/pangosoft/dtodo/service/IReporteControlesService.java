package xyz.pangosoft.dtodo.service;

/**
 * Los listados que Dtodo llevaba a mano en papel, ahora generados por el sistema: el control de cheques, el listado de clientes
 * despachados al crédito (con sus abonos y firma de recibido) y el listado de proformas con espacio para anotar cajas, paquetes
 * y la hora de empaque. Cada uno sale en PDF (para imprimir y llenar) y en Excel (una tabla editable).
 */
public interface IReporteControlesService {

    /**
     * Cheques recibidos por fecha de pago. El estado (recibido, depositado, cobrado, rechazado) y la sucursal son opcionales; el
     * cobrador solo obtiene los de su sucursal.
     */
    byte[] controlDeCheques(Integer idSucursal, String estado, String fechaInicio, String fechaFin, String formato);

    /**
     * Pedidos mayoristas a crédito con algo entregado, por fecha del pedido: monto, hasta tres abonos, saldo pendiente, forma de
     * pago y una columna para la firma de recibido.
     */
    byte[] despachadosAlCredito(Integer idRuta, boolean soloConSaldo, String fechaInicio, String fechaFin, String formato);

    /** Pedidos mayoristas vigentes por fecha, con columnas en blanco para cajas, paquetes o rollos y hora de empaque. */
    byte[] listadoDeProformas(Integer idRuta, String fechaInicio, String fechaFin, String formato);
}
