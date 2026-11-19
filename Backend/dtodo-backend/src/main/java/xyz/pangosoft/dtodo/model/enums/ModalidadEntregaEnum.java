package xyz.pangosoft.dtodo.model.enums;

/**
 * Cómo recibe el cliente un pedido mayorista. RUTA sale con el piloto de la ruta; TIENDA lo recoge el cliente en la tienda y
 * TRANSPORTE se envía por un transporte ajeno a la empresa. Solo la modalidad RUTA lleva piloto y entra a la nota de envío.
 */
public enum ModalidadEntregaEnum {
    RUTA, TIENDA, TRANSPORTE;

    /** Solo el pedido que sale por la ruta se carga al camión con un piloto. */
    public boolean llevaPiloto() {
        return this == RUTA;
    }
}
