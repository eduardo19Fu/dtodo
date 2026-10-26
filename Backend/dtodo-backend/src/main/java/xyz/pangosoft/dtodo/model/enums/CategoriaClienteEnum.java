package xyz.pangosoft.dtodo.model.enums;

/**
 * Categoría de descuento de un cliente mayorista: {@code A} (compra frecuente), {@code B} (no frecuente,
 * menos de 12 unidades) y {@code VOLUMEN} (frecuente, pide fardos o cientos). Los porcentajes de cada
 * categoría son configurables y viven en la tabla de descuentos, no aquí.
 */
public enum CategoriaClienteEnum {
    A,
    B,
    VOLUMEN;
}
