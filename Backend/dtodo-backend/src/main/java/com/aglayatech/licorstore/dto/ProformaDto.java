package com.aglayatech.licorstore.dto;

import java.math.BigDecimal;
import java.util.Date;

public interface ProformaDto {
    Integer getIdProforma();
    String getNoProforma();
    Date getFechaEmision();
    BigDecimal getTotal();
    String getEstado();
    String getUsuario();
    String getCliente();
}
