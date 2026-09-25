package xyz.pangosoft.dtodo.service;

import java.time.LocalDate;

import xyz.pangosoft.dtodo.model.enums.EstadoNotaCreditoEnum;

public interface IResumenNotasCreditoReporteService {

    byte[] generar(
            Integer idSucursal,
            LocalDate fechaInicio,
            LocalDate fechaFin,
            EstadoNotaCreditoEnum estado,
            String formato);
}
