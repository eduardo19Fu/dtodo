package xyz.pangosoft.dtodo.dto;

import java.math.BigDecimal;

import lombok.Data;
import lombok.NoArgsConstructor;

/** Un cliente con deuda en la cobranza: lo que debe repartido según su antigüedad. */
@Data
@NoArgsConstructor
public class CobranzaClienteDto {
    private Integer idCliente;
    private String cliente;
    private String negocio;
    private Integer idRuta;
    private String ruta;
    private BigDecimal limiteCredito;
    /** Lo que debe y todavía no vence (o no tiene plazo definido por no haberse entregado). */
    private BigDecimal porVencer = BigDecimal.ZERO;
    /** Lo vencido hasta 30 días. */
    private BigDecimal vencido1a30 = BigDecimal.ZERO;
    /** Lo vencido entre 31 y 60 días. */
    private BigDecimal vencido31a60 = BigDecimal.ZERO;
    /** Lo vencido hace más de 60 días. */
    private BigDecimal vencidoMasDe60 = BigDecimal.ZERO;
    /** Todo lo que debe: por vencer más vencido. */
    private BigDecimal total = BigDecimal.ZERO;
    /** Total de lo vencido. */
    private BigDecimal vencido = BigDecimal.ZERO;
    /** Días de atraso del pedido más vencido; 0 si no tiene nada vencido. */
    private int diasMaxAtraso;
}
