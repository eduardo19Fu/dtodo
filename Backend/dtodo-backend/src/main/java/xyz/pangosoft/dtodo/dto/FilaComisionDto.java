package xyz.pangosoft.dtodo.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.Data;
import lombok.NoArgsConstructor;

/** Un vendedor en el avance o la liquidación de comisiones de un año. */
@Data
@NoArgsConstructor
public class FilaComisionDto {
    private Integer idVendedor;
    private String vendedor;
    /** Cuándo se creó su usuario: ayuda al administrador a decidir si completó el año. */
    private LocalDateTime fechaAlta;
    private boolean activo;
    private BigDecimal montoEntregado = BigDecimal.ZERO;
    /** Monto mínimo de la meta alcanzada; nulo si no alcanzó ninguna. */
    private BigDecimal metaAlcanzada;
    /** Porcentaje de la meta alcanzada, 0 si no alcanzó ninguna. */
    private BigDecimal porcentaje = BigDecimal.ZERO;
    /** Lo que cobraría si completó el año (en un año sin liquidar) o lo que se liquidó. */
    private BigDecimal comision = BigDecimal.ZERO;
    /** Monto de la siguiente meta, nulo si ya alcanzó la más alta. */
    private BigDecimal siguienteMeta;
    /** Lo que le falta entregar para la siguiente meta. */
    private BigDecimal faltaParaSiguiente;
    /**
     * Cómo cuenta el año para este vendedor: NORMAL (de enero a diciembre), BLOQUE (ingresó el año anterior: se le paga ahora
     * todo lo entregado desde su ingreso, porque ya cumplió 365 días) o PENDIENTE (ingresó este año: aún no cumple 365 días y
     * se le paga en la liquidación del año siguiente).
     */
    private String situacion = "NORMAL";
    /** Desde cuándo cuenta lo entregado para este vendedor en el año. */
    private LocalDate periodoDesde;
    /** Solo en un año liquidado: el administrador marcó que completó el año. */
    private Boolean completoAnio;
}
