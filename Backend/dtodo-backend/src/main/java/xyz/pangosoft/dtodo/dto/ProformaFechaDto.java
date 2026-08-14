package xyz.pangosoft.dtodo.dto;

import java.math.BigDecimal;
import java.util.Date;

public interface ProformaFechaDto {
    Integer getIdProforma();
    String getNoProforma();
    Date getFechaEmision();
    BigDecimal getTotal();
    String getEstado();
    String getUsuario();
    String getCliente();
}
