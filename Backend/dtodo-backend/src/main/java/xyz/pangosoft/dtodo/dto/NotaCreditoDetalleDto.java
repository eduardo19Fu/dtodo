package xyz.pangosoft.dtodo.dto;

import xyz.pangosoft.dtodo.model.enums.EstadoNotaCreditoEnum;
import xyz.pangosoft.dtodo.model.enums.TipoDocumentoOrigenEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Proyección del detalle de una nota de crédito sin exponer entidades JPA.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class NotaCreditoDetalleDto {
    private Long idNotaCredito;
    private String correlativoFacturaSat;
    private String serieFacturaSat;
    private TipoDocumentoOrigenEnum tipoDocumentoOrigen;
    private String noProforma;
    private BigDecimal total;
    private String observaciones;
    private LocalDateTime fechaCreacion;
    private LocalDate fechaEntregaEstimada;
    private EstadoNotaCreditoEnum estado;
    private String cliente;
    private String vendedor;
    private List<NotaCreditoDetalleItemDto> items;
}
