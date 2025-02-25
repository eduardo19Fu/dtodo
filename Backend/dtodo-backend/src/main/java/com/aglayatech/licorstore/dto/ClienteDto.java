package com.aglayatech.licorstore.dto;

import java.time.LocalDateTime;

public interface ClienteDto {
    Integer getIdCliente();
    String getNombre();
    String getNit();
    String getDireccion();
    LocalDateTime getFechaRegistro();
    String getTelefono();
}
