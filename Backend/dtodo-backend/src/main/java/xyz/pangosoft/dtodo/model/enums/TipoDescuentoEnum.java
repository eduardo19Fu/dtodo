package xyz.pangosoft.dtodo.model.enums;

/**
 * De dónde sale el descuento de una línea de proforma. El usuario elige el tipo; el sistema solo lo sugiere.
 */
public enum TipoDescuentoEnum {
    /** Porcentaje de la categoría del cliente (A, B o un nivel de Volumen). */
    CATEGORIA,
    /** Porcentaje especial del producto al comprar por ciento. */
    CIENTO,
    /** Porcentaje especial del producto al comprar un fardo cerrado. */
    FARDO,
    /** Precio fijo del producto, expresado como el porcentaje equivalente sobre el precio de venta. */
    PRECIO_MAYORISTA,
    /** Porcentaje libre. */
    MANUAL;
}
