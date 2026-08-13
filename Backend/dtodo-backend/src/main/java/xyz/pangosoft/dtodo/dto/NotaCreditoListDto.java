package xyz.pangosoft.dtodo.dto;

import xyz.pangosoft.dtodo.model.enums.EstadoNotaCreditoEnum;
import xyz.pangosoft.dtodo.model.enums.TipoDocumentoOrigenEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Proyección utilizada para listar Notas de Crédito sin acarrear las relaciones
 * pesadas de la entidad ({@code items}, {@code cliente}, etc.).
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class NotaCreditoListDto {
    private Long idNotaCredito;
    private BigDecimal total;
    private String usuario;
    private String cliente;
    private String nitCliente;
    private String correlativoFacturaSat;
    private String serieFacturaSat;
    private TipoDocumentoOrigenEnum tipoDocumentoOrigen;
    private String noProforma;
    private LocalDateTime fechaCreacion;
    private LocalDate fechaEntregaEstimada;
    private EstadoNotaCreditoEnum estado;
}
