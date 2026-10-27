package xyz.pangosoft.dtodo.service;

/** Reporte "Comisiones del año" (PDF o Excel) con el avance o la liquidación de los vendedores. */
public interface IReporteComisionesService {

    /**
     * Genera el reporte del año. En un año sin liquidar muestra el avance (la comisión es lo que cobraría quien complete el
     * año y "Completó el año" queda sin definir); en uno liquidado, lo guardado.
     */
    byte[] comisionesDelAnio(int anio, String formato);
}
