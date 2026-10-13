package xyz.pangosoft.dtodo.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import xyz.pangosoft.dtodo.util.SoloAdministradorSerializer;

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
	@JsonSerialize(using = SoloAdministradorSerializer.class)
	private BigDecimal precioCompra;
	private LocalDateTime fechaActualizacion;
}
