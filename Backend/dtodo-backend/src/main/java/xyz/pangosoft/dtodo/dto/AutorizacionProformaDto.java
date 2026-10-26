package xyz.pangosoft.dtodo.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Una proforma en la bandeja de autorizaciones: por descuento sobre el tope o vencida. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AutorizacionProformaDto {

    public static final String TIPO_DESCUENTO = "DESCUENTO";
    public static final String TIPO_VENCIDA = "VENCIDA";

    private Long idProforma;
    private String noProforma;
    private Date fechaEmision;
    private BigDecimal total;
    private String cliente;
    private String ruta;
    private String vendedor;
    private LocalDate fechaVencimiento;
    /** Mayor porcentaje de descuento de la proforma. */
    private Float descuentoMaximo;
    /** {@link #TIPO_DESCUENTO} o {@link #TIPO_VENCIDA}. */
    private String tipo;
}
