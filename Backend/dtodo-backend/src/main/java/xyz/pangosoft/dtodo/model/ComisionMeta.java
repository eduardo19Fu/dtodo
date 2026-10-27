package xyz.pangosoft.dtodo.model;

import java.io.Serializable;
import java.math.BigDecimal;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Un nivel de comisión de un año: al entregar al menos {@code montoMinimo} en el año, el vendedor cobra
 * {@code porcentaje} % sobre todo lo entregado (porcentaje plano, no por tramos).
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "comisiones_metas")
public class ComisionMeta implements Serializable {

    private static final long serialVersionUID = 3517990224061182774L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idMeta;

    private Integer anio;

    private BigDecimal montoMinimo;

    private BigDecimal porcentaje;
}
