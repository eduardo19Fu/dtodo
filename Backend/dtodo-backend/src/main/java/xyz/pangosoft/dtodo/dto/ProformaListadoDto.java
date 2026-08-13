package xyz.pangosoft.dtodo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProformaListadoDto {
    private Long idProforma;
    private String noProforma;
    private Date fechaEmision;
    private BigDecimal total;
    private Integer idEstado;
    private String estado;
    private String usuario;
    private String vendedor;
    private String cliente;
    private String nitCliente;
}
