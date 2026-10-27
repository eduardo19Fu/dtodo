package xyz.pangosoft.dtodo.dto;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import lombok.Data;
import lombok.NoArgsConstructor;
import xyz.pangosoft.dtodo.model.enums.EstadoAutorizacionEnum;
import xyz.pangosoft.dtodo.model.enums.EstadoPedidoEnum;

/** Un pedido con sus líneas y sus etapas. Sin precios ni costos. */
@Data
@NoArgsConstructor
public class PedidoDetalleDto {

    private Long idProforma;
    private String noProforma;
    private Date fechaEmision;
    private LocalDate fechaEntregaEstimada;
    private String cliente;
    private String negocio;
    private String direccion;
    private String contacto;
    private String telefonoContacto;
    private String ruta;
    private String vendedor;
    private EstadoPedidoEnum estadoPedido;
    private EstadoAutorizacionEnum estadoAutorizacion;
    private Integer idRepartidor;
    private String repartidor;
    /** Observación general del pedido (cómo se empaca). */
    private String observaciones;
    /** Sucursal de la proforma: la que se propone para descontar el inventario en la entrega. */
    private Integer idSucursal;
    private String sucursal;
    private List<Linea> lineas = new ArrayList<>();
    private List<PedidoEtapaDto> etapas = new ArrayList<>();
    private List<EntregaDto> entregas = new ArrayList<>();

    @Data
    @NoArgsConstructor
    public static class Linea {
        private Long idDetalle;
        private String codigo;
        private String producto;
        private Integer cantidad;
        private String observacion;
        private Integer cantidadEntregada;
        /** Lo que falta por entregar. */
        private Integer pendiente;
    }
}
