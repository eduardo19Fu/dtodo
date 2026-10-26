package xyz.pangosoft.dtodo.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import xyz.pangosoft.dtodo.model.enums.AccionAuditoriaEnum;

/** Un renglón del historial de una proforma, listo para mostrarse. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProformaAuditoriaDto {
    private Long idAuditoria;
    private AccionAuditoriaEnum accion;
    /** Nombre de quien hizo la acción. */
    private String usuario;
    private String motivo;
    private String detalle;
    private LocalDateTime fecha;
}
