package xyz.pangosoft.dtodo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductoDto {
    private Integer idProducto;
    private String codProducto;
    private String nombre;
    private BigDecimal precioCompra;
    private BigDecimal precioVenta;
    private float porcentajeGanancia;
    private String descripcion;
    private Date fechaVencimiento;
    private Date fechaIngreso;
    private LocalDateTime fechaRegistro;
    private int stock;
    private String marcaProducto;
    private String tipoProducto;
    private String estado;
}
