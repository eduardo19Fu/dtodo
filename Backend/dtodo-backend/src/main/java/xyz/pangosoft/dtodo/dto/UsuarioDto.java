package xyz.pangosoft.dtodo.dto;

import java.time.LocalDateTime;
import java.util.List;

public class UsuarioDto {
    private int idUsuario;
    private String usuario;
    private String primerNombre;
    private String segundoNombre;
    private String apellido;
    private boolean enabled;
    private LocalDateTime fechaRegistro;
    private List<String> roles;
}
