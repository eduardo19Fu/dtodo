package xyz.pangosoft.dtodo.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Cuentas por cobrar de los clientes mayoristas al día de hoy. Cada pedido con algo pendiente cuenta como vencido cuando pasan
 * los días de crédito desde su primera entrega (un pedido de contado entregado sin cobrar vence el mismo día de la entrega).
 */
@Data
@NoArgsConstructor
public class CobranzaDto {
    private LocalDate fecha;
    private List<CobranzaClienteDto> clientes = new ArrayList<>();
    private BigDecimal porVencer = BigDecimal.ZERO;
    private BigDecimal vencido1a30 = BigDecimal.ZERO;
    private BigDecimal vencido31a60 = BigDecimal.ZERO;
    private BigDecimal vencidoMasDe60 = BigDecimal.ZERO;
    private BigDecimal total = BigDecimal.ZERO;
    private BigDecimal vencido = BigDecimal.ZERO;
    /** Cuántos de los clientes listados tienen algo vencido. */
    private int clientesConVencido;
}
