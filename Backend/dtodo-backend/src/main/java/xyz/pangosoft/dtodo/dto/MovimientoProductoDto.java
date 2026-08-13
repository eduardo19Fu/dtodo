package xyz.pangosoft.dtodo.dto;

import xyz.pangosoft.dtodo.model.enums.TipoMovimientoEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MovimientoProductoDto {
    private Long idMovimiento;
    private LocalDateTime fechaMovimiento;
    private Integer stockInicial;
    private TipoMovimientoEnum tipoMovimiento;
    private Integer cantidad;
    private String productoNombre;
    private String usuarioNombre;
}
