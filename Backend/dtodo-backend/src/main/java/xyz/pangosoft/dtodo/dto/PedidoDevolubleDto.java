package xyz.pangosoft.dtodo.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import lombok.Data;
import lombok.NoArgsConstructor;
import xyz.pangosoft.dtodo.model.enums.EstadoPedidoEnum;

/** Un pedido con lo entregado y lo que todavía se puede devolver, para armar una devolución. */
@Data
@NoArgsConstructor
public class PedidoDevolubleDto {
    private Long idProforma;
    private String noProforma;
    private EstadoPedidoEnum estadoPedido;
    /** Sucursal del pedido: la que se propone para recibir lo que reingresa. */
    private Integer idSucursal;
    private String sucursal;
    /** El pedido tiene una factura vigente: la devolución debe ligarse a una nota de crédito. */
    private boolean facturado;
    private Long noFactura;
    private List<Linea> lineas = new ArrayList<>();
    /** Notas de crédito vigentes del cliente que corresponden al pedido o a su factura. */
    private List<NotaCreditoOpcion> notasCredito = new ArrayList<>();

    @Data
    @NoArgsConstructor
    public static class Linea {
        private Long idDetalle;
        private String codigo;
        private String producto;
        private Integer entregada;
        private Integer devuelta;
        private Integer devolvible;
        /** Importe de la línea con descuento entre sus unidades. */
        private BigDecimal precioUnitario;
    }

    @Data
    @NoArgsConstructor
    public static class NotaCreditoOpcion {
        private Long idNotaCredito;
        private String descripcion;
        private BigDecimal total;
    }
}
