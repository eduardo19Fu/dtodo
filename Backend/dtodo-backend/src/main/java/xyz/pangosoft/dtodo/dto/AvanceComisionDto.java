package xyz.pangosoft.dtodo.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Comisiones de un año. En un año sin liquidar es un avance calculado en el momento; en uno liquidado muestra lo que
 * se guardó al liquidar, sin recalcular.
 */
@Data
@NoArgsConstructor
public class AvanceComisionDto {
    private Integer anio;
    /** El año ya terminó y se puede liquidar. */
    private boolean anioTerminado;
    private boolean liquidado;
    private LocalDateTime fechaLiquidacion;
    private String liquidadoPor;
    private List<MetaComisionDto> metas = new ArrayList<>();
    private List<FilaComisionDto> filas = new ArrayList<>();
    private BigDecimal totalEntregado = BigDecimal.ZERO;
    private BigDecimal totalComision = BigDecimal.ZERO;
}
