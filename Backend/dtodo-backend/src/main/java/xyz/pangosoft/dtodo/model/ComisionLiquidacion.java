package xyz.pangosoft.dtodo.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/** Lo que se liquidó a un vendedor en un año. Una vez guardado no se recalcula, aunque cambien los datos o las metas. */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString(exclude = { "vendedor", "usuario" })
@Entity
@Table(name = "comisiones_liquidaciones")
public class ComisionLiquidacion implements Serializable {

    private static final long serialVersionUID = -7403315982716604815L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idLiquidacion;

    private Integer anio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_vendedor")
    private Usuario vendedor;

    private BigDecimal montoEntregado;

    /** Monto mínimo de la meta alcanzada; nulo si no alcanzó ninguna o no completó el año. */
    private BigDecimal montoMeta;

    private BigDecimal porcentaje;

    private BigDecimal comision;

    /** El administrador marcó que el vendedor trabajó el año completo; sin eso la comisión es cero. */
    private boolean completoAnio;

    /** Quien liquidó. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario")
    private Usuario usuario;

    private LocalDateTime fecha;
}
