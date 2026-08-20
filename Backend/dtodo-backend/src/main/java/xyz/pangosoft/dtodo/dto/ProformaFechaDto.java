package xyz.pangosoft.dtodo.dto;

import java.math.BigDecimal;
import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProformaFechaDto {
    private Long idProforma;
    private String noProforma;
    private Date fechaEmision;
    private BigDecimal total;
    private String estado;
    private String usuario;
    private String cliente;
}
