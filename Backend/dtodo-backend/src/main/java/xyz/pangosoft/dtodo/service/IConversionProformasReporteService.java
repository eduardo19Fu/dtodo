package xyz.pangosoft.dtodo.service;

import java.time.LocalDate;

public interface IConversionProformasReporteService {

    byte[] generar(
            Integer idSucursal,
            LocalDate fechaInicio,
            LocalDate fechaFin,
            Integer idUsuario,
            String formato);
}
