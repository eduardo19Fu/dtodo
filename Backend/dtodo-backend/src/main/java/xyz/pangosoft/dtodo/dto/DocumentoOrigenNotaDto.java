package xyz.pangosoft.dtodo.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DocumentoOrigenNotaDto {

    private Long idDocumento;
    private Integer idCliente;
    private String cliente;
    private String numero;
    private String serie;
}
