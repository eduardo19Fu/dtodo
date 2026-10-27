package xyz.pangosoft.dtodo.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Un nivel de comisión: desde {@code montoMinimo} entregado en el año, {@code porcentaje} % sobre todo lo entregado. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MetaComisionDto {
    private BigDecimal montoMinimo;
    private BigDecimal porcentaje;
}
