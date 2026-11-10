package xyz.pangosoft.dtodo.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.Data;
import lombok.NoArgsConstructor;

/** Un cheque tal como se muestra en el control de cheques y en el detalle de su recibo. */
@Data
@NoArgsConstructor
public class ChequeDto {
    private Long idCheque;
    private Long idRecibo;
    private String numeroRecibo;
    private Integer idSucursal;
    private String sucursal;
    private Integer idCliente;
    private String cliente;
    private BigDecimal monto;
    private LocalDate fechaPago;
    private String banco;
    private String numeroCheque;
    private String girador;
    private LocalDate fechaCheque;
    /** Verdadero si la fecha del cheque es posterior a hoy: todavía no se puede depositar. */
    private boolean postfechado;
    private String notas;
    private String estado;
    private LocalDateTime fechaEstado;
    private String usuarioEstado;
    private String motivoRechazo;
}
