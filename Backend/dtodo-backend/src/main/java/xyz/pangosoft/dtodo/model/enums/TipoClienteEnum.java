package xyz.pangosoft.dtodo.model.enums;

/**
 * Tipo de cliente. Todo cliente existente es {@link #MINORISTA}; el mayorista se asigna a una ruta y es
 * quien recibe proformas mayoristas con descuentos por categoría o por cantidad.
 */
public enum TipoClienteEnum {
    MINORISTA,
    MAYORISTA;
}
