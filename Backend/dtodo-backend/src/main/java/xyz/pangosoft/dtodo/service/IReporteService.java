package xyz.pangosoft.dtodo.service;

public interface IReporteService {

    byte[] generarPolizaIndividual(Integer idSucursal, Integer idUsuario, String fechaInicio, String fechaFin);

    byte[] generarPolizaGeneral(Integer idSucursal, String fechaInicio, String fechaFin);

    byte[] generarVentasProducto(
            Integer idSucursal,
            String fechaInicio,
            String fechaFin,
            Integer idCategoria,
            String formato);

    byte[] generarVentasCliente(
            Integer idSucursal,
            String fechaInicio,
            String fechaFin,
            Integer idCliente,
            String formato);

    byte[] generarRentabilidadProducto(
            Integer idSucursal,
            String fechaInicio,
            String fechaFin,
            Integer idCategoria,
            String formato);

    byte[] generarMovimientosInventario(
            Integer idSucursal,
            String fechaInicio,
            String fechaFin,
            String formato);

    byte[] generarExistencias(Integer idSucursal);

    byte[] generarBajoStock(Integer idSucursal, Integer idCategoria, String formato);

    byte[] generarKardexProducto(
            Integer idSucursal,
            Integer idProducto,
            String fechaInicio,
            String fechaFin,
            String formato);

    byte[] generarProformas(Integer idUsuario, String fechaInicio, String fechaFin, boolean todas);

    byte[] generarResumenNotasCredito(
            Integer idSucursal,
            String fechaInicio,
            String fechaFin,
            String estado,
            String formato);

    byte[] generarComprasPeriodo(
            Integer idSucursal,
            String fechaInicio,
            String fechaFin,
            Integer idProveedor,
            String estado,
            String formato);
}
