package xyz.pangosoft.dtodo.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.Data;
import lombok.NoArgsConstructor;

/** Un recibo de caja tal como se muestra en la lista y el detalle. */
@Data
@NoArgsConstructor
public class ReciboCajaDto {
    private Long idRecibo;
    private Long numero;
    /** Número con el que se identifica el recibo en papel: la sucursal y su número, por ejemplo "S2-000015". */
    private String numeroVisible;
    private Integer idSucursal;
    private String sucursal;
    private Integer idCliente;
    private String cliente;
    private Long idProforma;
    private String noProforma;
    private Long idFactura;
    private Long noFactura;
    private BigDecimal monto;
    private String formaPago;
    private String referencia;
    private String observaciones;
    private LocalDate fechaPago;
    private LocalDateTime fechaRegistro;
    private String usuario;
    private boolean anulado;
    private LocalDateTime fechaAnulacion;
    private String usuarioAnulacion;
    private String motivoAnulacion;
}
