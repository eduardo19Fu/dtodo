package xyz.pangosoft.dtodo.service;

import java.time.LocalDate;

public interface IKardexProductoReporteService {

    byte[] generar(
            Integer idSucursal,
            Integer idProducto,
            LocalDate fechaInicio,
            LocalDate fechaFin,
            String formato);
}
