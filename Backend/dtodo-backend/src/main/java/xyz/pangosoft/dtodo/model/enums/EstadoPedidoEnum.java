package xyz.pangosoft.dtodo.model.enums;

import java.util.Arrays;
import java.util.List;

/**
 * Etapa de un pedido mayorista (una proforma mayorista confirmada). Una proforma minorista no tiene etapas.
 * {@link #ENTREGADA_PARCIAL} y {@link #ENTREGADA} se alcanzan al registrar la entrega, que llega en la fase 2B.
 */
public enum EstadoPedidoEnum {
    CONFIRMADA,
    EN_PREPARACION,
    EMPACADA,
    EN_RUTA,
    ENTREGADA_PARCIAL,
    ENTREGADA;

    /** Etapas en las que el pedido todavía está por entregarse (completa o lo que quedó pendiente). */
    public static final List<EstadoPedidoEnum> POR_ENTREGAR = Arrays.asList(
            CONFIRMADA, EN_PREPARACION, EMPACADA, EN_RUTA, ENTREGADA_PARCIAL);

    /** La etapa que sigue cuando bodega avanza el pedido; vacío si lo siguiente es la entrega (fase 2B). */
    public EstadoPedidoEnum siguienteEnBodega() {
        switch (this) {
            case CONFIRMADA:
                return EN_PREPARACION;
            case EN_PREPARACION:
                return EMPACADA;
            case EMPACADA:
                return EN_RUTA;
            default:
                return null;
        }
    }

    /** La etapa a la que se vuelve un paso atrás; nulo si ya está al principio o si ya se entregó. */
    public EstadoPedidoEnum anterior() {
        switch (this) {
            case EN_PREPARACION:
                return CONFIRMADA;
            case EMPACADA:
                return EN_PREPARACION;
            case EN_RUTA:
                return EMPACADA;
            default:
                return null;
        }
    }

    /** Ya se entregó, completa o en parte: el pedido deja de poder modificarse. */
    public boolean entregado() {
        return this == ENTREGADA_PARCIAL || this == ENTREGADA;
    }
}
