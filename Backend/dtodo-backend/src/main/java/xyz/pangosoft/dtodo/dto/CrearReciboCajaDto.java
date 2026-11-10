package xyz.pangosoft.dtodo.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Lo que se manda para emitir un recibo de caja. La sucursal solo la puede elegir el administrador; para los demás el
 * recibo sale siempre de su propia sucursal.
 */
@Data
@NoArgsConstructor
public class CrearReciboCajaDto {
    private Integer idCliente;
    /** Opcional: el pedido al que paga. */
    private Long idProforma;
    /** Opcional: la factura a la que paga. */
    private Long idFactura;
    private BigDecimal monto;
    /** EFECTIVO, DEPOSITO, TRANSFERENCIA o CHEQUE. */
    private String formaPago;
    private String referencia;
    private String observaciones;
    /** Fecha en que el cliente pagó; hoy si no se manda. */
    private LocalDate fechaPago;
    private Integer idSucursal;
    /** Solo si la forma de pago es CHEQUE: banco, número, fecha y demás datos del cheque. */
    private DatosChequeDto cheque;
}
