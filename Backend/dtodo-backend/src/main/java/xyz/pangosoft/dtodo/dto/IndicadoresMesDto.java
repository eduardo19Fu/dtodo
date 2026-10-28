package xyz.pangosoft.dtodo.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Los indicadores de un mes: ventas por vendedor (lo entregado menos lo devuelto), pedidos entregados a tiempo y cuentas por
 * cobrar al día de hoy.
 */
@Data
@NoArgsConstructor
public class IndicadoresMesDto {
    private int anio;
    private int mes;
    private LocalDate desde;
    /** Último día del mes. */
    private LocalDate hasta;
    private List<VentaVendedor> vendedores = new ArrayList<>();
    private BigDecimal totalEntregado = BigDecimal.ZERO;
    private BigDecimal totalDevuelto = BigDecimal.ZERO;
    private BigDecimal totalNeto = BigDecimal.ZERO;
    /** Ventas netas del mes anterior, para compararlas. */
    private BigDecimal netoMesAnterior = BigDecimal.ZERO;
    /** Pedidos entregados por completo en el mes que tenían fecha de entrega estimada. */
    private int pedidosConFecha;
    private int pedidosATiempo;
    /** Porcentaje (0 a 100) de pedidos entregados a tiempo; nulo si no hubo pedidos con fecha estimada. */
    private BigDecimal porcentajeATiempo;
    private CuentasPorCobrar cuentasPorCobrar = new CuentasPorCobrar();

    @Data
    @NoArgsConstructor
    public static class VentaVendedor {
        private Integer idVendedor;
        private String vendedor;
        private BigDecimal entregado = BigDecimal.ZERO;
        private BigDecimal devuelto = BigDecimal.ZERO;
        private BigDecimal neto = BigDecimal.ZERO;
        /** Cuánto representa de las ventas netas del mes, en por ciento. */
        private BigDecimal participacion = BigDecimal.ZERO;
        private int pedidosConFecha;
        private int pedidosATiempo;
        private BigDecimal porcentajeATiempo;
    }

    @Data
    @NoArgsConstructor
    public static class CuentasPorCobrar {
        private BigDecimal total = BigDecimal.ZERO;
        private BigDecimal porVencer = BigDecimal.ZERO;
        private BigDecimal vencido = BigDecimal.ZERO;
        private int clientesConDeuda;
        private int clientesConVencido;
    }
}
