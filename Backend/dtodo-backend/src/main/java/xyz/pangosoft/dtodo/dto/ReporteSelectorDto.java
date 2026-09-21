package xyz.pangosoft.dtodo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReporteSelectorDto {

    private Integer valor;
    private String etiqueta;
    private String detalle;
}
