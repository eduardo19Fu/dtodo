package xyz.pangosoft.dtodo.service;

import java.time.LocalDate;

public interface IComprasProveedorProductoReporteService {

    byte[] generar(Integer idSucursal, LocalDate fechaInicio, LocalDate fechaFin, Integer idProveedor);
}
