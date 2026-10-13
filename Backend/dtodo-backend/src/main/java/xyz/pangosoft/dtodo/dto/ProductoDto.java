package xyz.pangosoft.dtodo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import xyz.pangosoft.dtodo.util.SoloAdministradorSerializer;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductoDto {
    private Integer idProducto;
    private String codProducto;
    private String nombre;
    @JsonSerialize(using = SoloAdministradorSerializer.class)
    private BigDecimal precioCompra;
    private BigDecimal precioVenta;
    @JsonSerialize(using = SoloAdministradorSerializer.class)
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
