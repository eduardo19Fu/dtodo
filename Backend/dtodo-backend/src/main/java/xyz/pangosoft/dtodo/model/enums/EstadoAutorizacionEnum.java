package xyz.pangosoft.dtodo.model.enums;

/**
 * Estado de autorización de una proforma mayorista. Una proforma que no necesita autorización guarda {@code null}:
 * {@link #PENDIENTE} y {@link #RECHAZADA} impiden facturarla.
 */
public enum EstadoAutorizacionEnum {
    PENDIENTE,
    APROBADA,
    RECHAZADA;
}
