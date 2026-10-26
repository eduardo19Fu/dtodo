package xyz.pangosoft.dtodo.dto;

import java.time.LocalDate;
import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import xyz.pangosoft.dtodo.model.enums.EstadoAutorizacionEnum;
import xyz.pangosoft.dtodo.model.enums.EstadoPedidoEnum;

/** Una fila de la lista de pedidos. No lleva importes: la lista es operativa (bodega y entrega). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PedidoDto {
    private Long idProforma;
    private String noProforma;
    private Date fechaEmision;
    private LocalDate fechaEntregaEstimada;
    private String cliente;
    private String negocio;
    private Integer idRuta;
    private String ruta;
    private String vendedor;
    private EstadoPedidoEnum estadoPedido;
    /** Total de unidades del pedido. */
    private Long unidades;
    /** PENDIENTE o RECHAZADA impiden que el pedido avance. */
    private EstadoAutorizacionEnum estadoAutorizacion;
}
