package com.aglayatech.licorstore.dto;

import com.aglayatech.licorstore.model.enums.EstadoNotaCreditoEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NotaCreditoListDto {
    private Long idNotaCredito;
    private BigDecimal total;
    private String usuario;
    private String correlativoFacturaSat;
    private String serieFacturaSat;
    private LocalDateTime fechaCreacion;
    private LocalDate fechaEntregaEstimada;
    private EstadoNotaCreditoEnum estado;
}
