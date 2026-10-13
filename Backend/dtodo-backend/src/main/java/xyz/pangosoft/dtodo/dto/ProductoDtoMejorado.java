package xyz.pangosoft.dtodo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import xyz.pangosoft.dtodo.util.SoloAdministradorSerializer;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProductoDtoMejorado {
    private int idProducto;
    private String codProducto;
    private String nombre;
    @JsonSerialize(using = SoloAdministradorSerializer.class)
    private BigDecimal precioCompra;
    private BigDecimal precioVenta;
    @JsonSerialize(using = SoloAdministradorSerializer.class)
    private float porcentajeGanancia;
    private String descripcion;
    private LocalDate fechaVencimiento;
    private LocalDate fechaIngreso;
    private LocalDateTime fechaRegistro;
    private int stock;
    private String imagen;
    private int idestado;
    private String marcaProducto;
    private String tipoProducto;
    private String estado;
}
