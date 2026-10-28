package xyz.pangosoft.dtodo.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Date;

import lombok.Data;
import lombok.NoArgsConstructor;

/** Una proforma en la bandeja de autorizaciones: por descuento sobre el tope, vencida o por pasar del límite de crédito. */
@Data
@NoArgsConstructor
public class AutorizacionProformaDto {

    public static final String TIPO_DESCUENTO = "DESCUENTO";
    public static final String TIPO_VENCIDA = "VENCIDA";
    public static final String TIPO_CREDITO = "CREDITO";

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
    /** {@link #TIPO_DESCUENTO}, {@link #TIPO_VENCIDA} o {@link #TIPO_CREDITO}. */
    private String tipo;
    /** Solo en {@link #TIPO_CREDITO}: cuánto pasa del límite de crédito del cliente. */
    private BigDecimal excesoCredito;
    /** Solo en {@link #TIPO_CREDITO}: el límite de crédito del cliente. */
    private BigDecimal limiteCredito;

    public AutorizacionProformaDto(Long idProforma, String noProforma, Date fechaEmision, BigDecimal total, String cliente,
            String ruta, String vendedor, LocalDate fechaVencimiento, Float descuentoMaximo, String tipo) {
        this.idProforma = idProforma;
        this.noProforma = noProforma;
        this.fechaEmision = fechaEmision;
        this.total = total;
        this.cliente = cliente;
        this.ruta = ruta;
        this.vendedor = vendedor;
        this.fechaVencimiento = fechaVencimiento;
        this.descuentoMaximo = descuentoMaximo;
        this.tipo = tipo;
    }

    public AutorizacionProformaDto(Long idProforma, String noProforma, Date fechaEmision, BigDecimal total, String cliente,
            String ruta, String vendedor, LocalDate fechaVencimiento, Float descuentoMaximo, String tipo,
            BigDecimal excesoCredito, BigDecimal limiteCredito) {
        this(idProforma, noProforma, fechaEmision, total, cliente, ruta, vendedor, fechaVencimiento, descuentoMaximo, tipo);
        this.excesoCredito = excesoCredito;
        this.limiteCredito = limiteCredito;
    }
}
