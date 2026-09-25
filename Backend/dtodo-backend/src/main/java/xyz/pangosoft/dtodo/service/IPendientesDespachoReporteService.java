package xyz.pangosoft.dtodo.service;

import java.time.LocalDate;

public interface IPendientesDespachoReporteService {

    byte[] generar(
            Integer idSucursal,
            LocalDate fechaInicio,
            LocalDate fechaFin,
            Integer idCliente,
            String formato);
}
