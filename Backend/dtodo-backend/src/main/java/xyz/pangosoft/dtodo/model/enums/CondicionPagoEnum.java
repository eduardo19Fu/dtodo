package xyz.pangosoft.dtodo.model.enums;

/**
 * Cómo paga el cliente un pedido mayorista. Al contado se cobra con un recibo de caja ligado al pedido antes de entregar;
 * a crédito solo puede comprar un cliente con límite y días de crédito, y la deuda nace al entregar.
 */
public enum CondicionPagoEnum {
    CONTADO,
    CREDITO
}
