package xyz.pangosoft.dtodo.model.enums;

/** Qué se hizo sobre una proforma, para el historial de auditoría. */
public enum AccionAuditoriaEnum {
    CREACION,
    MODIFICACION,
    ANULACION,
    AUTORIZACION,
    RECHAZO,
    /** Un administrador autorizó facturar la proforma aunque ya venció. */
    AUTORIZACION_VENCIDA,
    CAMBIO_CLIENTE_FACTURA;
}
