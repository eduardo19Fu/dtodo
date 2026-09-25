package xyz.pangosoft.dtodo.service;

import java.time.LocalDate;

public interface IVentasProductoReporteService {

    byte[] generar(
            Integer idSucursal,
            LocalDate fechaInicio,
            LocalDate fechaFin,
            Integer idCategoria,
            String formato);
}
