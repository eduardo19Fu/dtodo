package xyz.pangosoft.dtodo.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TipoProductoDto {
    private Integer idTipoProducto;
    private String tipoProducto;
    private LocalDateTime fechaRegistro;
    private String usuario;
}
