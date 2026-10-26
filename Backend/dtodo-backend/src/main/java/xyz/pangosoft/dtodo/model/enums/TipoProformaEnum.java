package xyz.pangosoft.dtodo.model.enums;

/**
 * Tipo de proforma. Es independiente del tipo del cliente: un cliente mayorista que compra unidades en la tienda
 * recibe una proforma {@link #MINORISTA}; la {@link #MAYORISTA} lleva ruta, vigencia, observaciones y el formato
 * impreso propio, y solo se hace a un cliente mayorista.
 */
public enum TipoProformaEnum {
    MINORISTA,
    MAYORISTA;
}
