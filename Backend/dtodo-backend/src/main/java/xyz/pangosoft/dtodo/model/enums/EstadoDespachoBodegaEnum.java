package xyz.pangosoft.dtodo.model.enums;

/**
 * Ciclo de vida de un despacho de bodega.
 *
 * <ul>
 *   <li>{@link #PENDIENTE}: pendiente de aprobación. El stock ya salió de la bodega (queda reservado) pero
 *       todavía no ingresa a la sucursal destino.</li>
 *   <li>{@link #REALIZADO}: aprobado; el stock ingresó a la sucursal destino. Estado final.</li>
 *   <li>{@link #CANCELADO}: cancelado estando pendiente; el stock reservado regresó a la bodega. Estado final.</li>
 * </ul>
 */
public enum EstadoDespachoBodegaEnum {
    PENDIENTE,
    REALIZADO,
    CANCELADO;
}
