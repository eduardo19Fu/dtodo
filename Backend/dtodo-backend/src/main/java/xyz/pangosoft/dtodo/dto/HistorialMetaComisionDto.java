package xyz.pangosoft.dtodo.dto;

import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Un cambio de las metas de un año, con las metas de antes y de después. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HistorialMetaComisionDto {
    /** CREAR, CAMBIAR o COPIAR. */
    private String accion;
    private String usuario;
    private LocalDateTime fecha;
    private String motivo;
    private List<MetaComisionDto> antes;
    private List<MetaComisionDto> despues;
}
