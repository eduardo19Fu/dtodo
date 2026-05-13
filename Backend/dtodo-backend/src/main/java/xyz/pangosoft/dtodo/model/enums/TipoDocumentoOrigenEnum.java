package xyz.pangosoft.dtodo.model.enums;

/**
 * Discrimina el documento origen sobre el que se emite una Nota de Crédito.
 *
 * <ul>
 *   <li>{@link #FACTURA}: La nota de crédito hace referencia a una Factura emitida
 *       (identificada por correlativo y serie SAT).</li>
 *   <li>{@link #PROFORMA}: La nota de crédito hace referencia a una Proforma
 *       (identificada por su número de proforma).</li>
 * </ul>
 */
public enum TipoDocumentoOrigenEnum {
    FACTURA,
    PROFORMA
}
