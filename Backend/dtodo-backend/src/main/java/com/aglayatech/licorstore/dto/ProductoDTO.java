package com.aglayatech.licorstore.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductoDTO {
    private Integer idProducto;
    private String codProducto;
    private String nombre;
    private BigDecimal precioCompra;
    private BigDecimal precioVenta;
    private float porcentajeGanancia;
    private String imagen;
    private String descripcion;
    private String link;
    private Date fechaVencimiento;
    private Date fechaIngreso;
    private Date fechaRegistro;
    private int stock;
}
