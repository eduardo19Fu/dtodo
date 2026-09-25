package xyz.pangosoft.dtodo.service;

public interface IBajoStockReporteService {

    byte[] generar(Integer idSucursal, Integer idCategoria, String formato);
}
