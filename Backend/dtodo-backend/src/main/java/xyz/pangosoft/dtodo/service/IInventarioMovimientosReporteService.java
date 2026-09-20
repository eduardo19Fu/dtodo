package xyz.pangosoft.dtodo.service;

import java.time.LocalDate;

public interface IInventarioMovimientosReporteService {

    byte[] generar(
            Integer idSucursal,
            LocalDate fechaInicio,
            LocalDate fechaFin,
            String formato);
}
