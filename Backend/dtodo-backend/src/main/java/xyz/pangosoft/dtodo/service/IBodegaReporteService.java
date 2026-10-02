package xyz.pangosoft.dtodo.service;

import java.time.LocalDate;

import xyz.pangosoft.dtodo.model.enums.EstadoDespachoBodegaEnum;

/** Reportes del módulo de bodegas y el comprobante imprimible de cada despacho. */
public interface IBodegaReporteService {

    /** Comprobante del despacho en PDF. */
    byte[] generarComprobanteDespacho(Long idDespacho);

    /** Existencias actuales de una bodega, valorizadas al costo de compra. */
    byte[] generarExistencias(Integer idBodega, String formato);

    /** Bitácora de movimientos de una bodega durante el período indicado. */
    byte[] generarMovimientos(Integer idBodega, LocalDate fechaInicio, LocalDate fechaFin, String formato);

    /** Despachos del período; {@code idBodega} y {@code estado} son filtros opcionales. */
    byte[] generarDespachos(
            Integer idBodega,
            LocalDate fechaInicio,
            LocalDate fechaFin,
            EstadoDespachoBodegaEnum estado,
            String formato);
}
