package xyz.pangosoft.dtodo.dto;

import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UsuarioDto {
    private Integer idUsuario;
    private String usuario;
    private String primerNombre;
    private String segundoNombre;
    private String apellido;
    private boolean enabled;
    private LocalDateTime fechaRegistro;
    private List<String> roles;

    public UsuarioDto(Integer idUsuario, String usuario, String primerNombre,
                      String segundoNombre, String apellido, boolean enabled,
                      LocalDateTime fechaRegistro) {
        this.idUsuario = idUsuario;
        this.usuario = usuario;
        this.primerNombre = primerNombre;
        this.segundoNombre = segundoNombre;
        this.apellido = apellido;
        this.enabled = enabled;
        this.fechaRegistro = fechaRegistro;
    }
}
