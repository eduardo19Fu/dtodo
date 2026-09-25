package xyz.pangosoft.dtodo.service;

import java.time.LocalDate;

import xyz.pangosoft.dtodo.model.enums.EstadoCompraEnum;

public interface IComprasPeriodoReporteService {

    byte[] generar(
            Integer idSucursal,
            LocalDate fechaInicio,
            LocalDate fechaFin,
            Integer idProveedor,
            EstadoCompraEnum estado,
            String formato);
}
