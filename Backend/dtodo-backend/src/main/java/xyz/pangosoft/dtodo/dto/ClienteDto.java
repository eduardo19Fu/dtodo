package xyz.pangosoft.dtodo.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ClienteDto {
    private Integer idCliente;
    private String nombre;
    private String nit;
    private String direccion;
    private LocalDateTime fechaRegistro;
    private String telefono;
}
