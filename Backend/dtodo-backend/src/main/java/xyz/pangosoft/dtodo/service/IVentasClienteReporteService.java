package xyz.pangosoft.dtodo.service;

import java.time.LocalDate;

public interface IVentasClienteReporteService {

    byte[] generar(
            Integer idSucursal,
            LocalDate fechaInicio,
            LocalDate fechaFin,
            Integer idCliente,
            String formato);
}
