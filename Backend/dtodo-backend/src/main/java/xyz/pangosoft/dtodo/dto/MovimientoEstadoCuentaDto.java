package xyz.pangosoft.dtodo.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Un renglón del estado de cuenta de un cliente: una entrega (cargo) o un recibo (abono) con el saldo corrido. Los atributos
 * con nombre {@code fechaTexto} son para el reporte impreso.
 */
@Data
@NoArgsConstructor
public class MovimientoEstadoCuentaDto {
    private LocalDate fecha;
    private String fechaTexto;
    private String documento;
    private String detalle;
    private BigDecimal cargo;
    private BigDecimal abono;
    private BigDecimal saldo;
}
