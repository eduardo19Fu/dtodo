package xyz.pangosoft.dtodo.service;

/**
 * Reportes de la Fase 1 de clientes mayoristas. Ninguno depende de la entrega del pedido. Fechas en formato
 * {@code yyyy-MM-dd}; formato {@code PDF} (por omisión) o {@code XLSX}. Una ruta nula significa todas las rutas.
 */
public interface IReporteMayoristasService {

    /** Clientes mayoristas agrupados por ruta, con su vendedor y datos de contacto. */
    byte[] clientesPorRuta(Integer idRuta, String formato);

    /**
     * Clientes mayoristas que no compraron en el período. Se considera compra una proforma o una factura no
     * anulada; el reporte muestra también la última compra de cada uno.
     */
    byte[] clientesSinCompras(Integer idRuta, String fechaInicio, String fechaFin, String formato);

    /**
     * Ventas entregadas por ruta y, dentro de cada ruta, por vendedor. Cuenta solo lo entregado al cliente: cada unidad
     * entregada a su precio de línea con descuento, en la fecha real de la entrega. Una entrega parcial suma solo lo
     * entregado y las entregas anuladas no cuentan. Ruta y vendedor son opcionales (nulos = todos).
     */
    byte[] entregadoPorRutaYVendedor(Integer idRuta, Integer idVendedor, String fechaInicio, String fechaFin, String formato);

    /**
     * Proformas mayoristas generadas y facturadas por cada vendedor en el período. Facturada es tener una factura
     * vigente (no anulada), aunque se haya emitido después del período.
     */
    byte[] proformasPorVendedor(Integer idRuta, String fechaInicio, String fechaFin, String formato);
}
