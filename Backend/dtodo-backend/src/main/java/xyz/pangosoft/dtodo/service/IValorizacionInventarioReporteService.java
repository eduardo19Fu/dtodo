package xyz.pangosoft.dtodo.service;

import java.time.LocalDate;

public interface IValorizacionInventarioReporteService {

    byte[] generar(Integer idSucursal, LocalDate fechaCorte, String formato);
}
