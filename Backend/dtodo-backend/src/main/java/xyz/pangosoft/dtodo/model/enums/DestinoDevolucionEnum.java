package xyz.pangosoft.dtodo.model.enums;

/** Qué se hace con el producto que el cliente devuelve. */
public enum DestinoDevolucionEnum {
    /** Vuelve al inventario de la sucursal que lo recibe. */
    REINGRESO,
    /** No vuelve al inventario (merma): se registra la devolución, pero no entra stock. */
    BAJA
}
