package xyz.pangosoft.dtodo.model.enums;

/**
 * Tipos de movimiento que puede registrar una bodega sobre sus existencias.
 *
 * <p>Cada tipo indica si suma o resta existencias, de modo que el servicio de inventario de bodega no
 * necesita un {@code switch} por tipo para calcular el nuevo saldo.</p>
 */
public enum TipoMovimientoBodegaEnum {

    /** Ingreso manual de un producto a la bodega. */
    INGRESO(true),
    /** Carga masiva de existencias (clonado desde otra sucursal o bodega, o archivo Excel). */
    IMPORTACION(true),
    /** Reintegro de un despacho cancelado antes de ser aprobado. */
    ANULACION_DESPACHO(true),
    /** Reducción manual de existencias (merma, daño, ajuste). */
    REDUCCION(false),
    /** Retiro del producto del inventario de la bodega; descuenta toda la existencia restante. */
    ELIMINACION(false),
    /** Salida de producto hacia una sucursal mediante un despacho. */
    DESPACHO(false);

    private final boolean incrementaStock;

    TipoMovimientoBodegaEnum(boolean incrementaStock) {
        this.incrementaStock = incrementaStock;
    }

    public boolean incrementaStock() {
        return incrementaStock;
    }
}
