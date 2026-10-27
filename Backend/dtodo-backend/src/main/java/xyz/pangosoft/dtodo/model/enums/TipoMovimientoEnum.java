package xyz.pangosoft.dtodo.model.enums;

public enum TipoMovimientoEnum {
    VENTA,
    ENTRADA,
    SALIDA,
    COMPRA,
    ANULACION_FACTURA,
    ELIMINAR_COMPRA,
    ANULACION_NOTA,
    ENTREGA_PRODUCTO_NOTA,
    /** Salida por la entrega de un pedido mayorista al cliente. */
    ENTREGA_PROFORMA,
    /** Reingreso por anular una entrega de pedido mayorista. */
    ANULACION_ENTREGA_PROFORMA;

}
