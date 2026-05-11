package com.aglayatech.licorstore.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DespachoNotaDto {
    private Long idDespacho;
    private String idEvento;
    private LocalDateTime fechaDespacho;
    private String codProducto;
    private Integer idProducto;
    private String productoNombre;
    private Integer cantidad;
    private BigDecimal totalDespacho;
    private String usuario;
}
