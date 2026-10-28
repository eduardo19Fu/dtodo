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
    ANULACION_ENTREGA_PROFORMA,
    /** Reingreso al inventario de lo que un cliente devuelve de un pedido mayorista. */
    DEVOLUCION_CLIENTE,
    /** Salida del inventario de lo que había reingresado por una devolución de cliente que se anuló. */
    ANULACION_DEVOLUCION_CLIENTE;

}
