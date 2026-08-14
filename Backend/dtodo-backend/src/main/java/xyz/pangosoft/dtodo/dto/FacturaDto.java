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
public class FacturaDto {
    private Long idFactura;
    private Long noFactura;
    private String serie;
    private Date fecha;
    private BigDecimal total;
    private Integer idEstado;
    private String estado;
    private String usuario;
    private String vendedor;
    private String cliente;
    private String nitCliente;
    private String certificacionSat;
}
