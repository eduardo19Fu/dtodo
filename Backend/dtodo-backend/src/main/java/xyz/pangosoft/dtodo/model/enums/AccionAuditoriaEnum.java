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
    /** Un administrador autorizó un pedido que pasa del límite de crédito del cliente. */
    AUTORIZACION_CREDITO,
    RECHAZO_CREDITO,
    CAMBIO_CLIENTE_FACTURA;
}
