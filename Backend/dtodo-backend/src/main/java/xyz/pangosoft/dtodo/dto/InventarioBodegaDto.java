package xyz.pangosoft.dtodo.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class InventarioBodegaDto {
	private Long idInventarioBodega;
	private Integer idProducto;
	private String codProducto;
	private String nombreProducto;
	private int stock;
	private Integer stockMinimo;
	private BigDecimal precioCompra;
	private LocalDateTime fechaActualizacion;
}
