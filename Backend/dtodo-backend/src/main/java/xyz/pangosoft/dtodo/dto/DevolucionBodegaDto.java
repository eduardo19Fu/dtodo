package xyz.pangosoft.dtodo.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import xyz.pangosoft.dtodo.model.enums.EstadoDespachoBodegaEnum;
import xyz.pangosoft.dtodo.util.SoloAdministradorSerializer;

/** Fila del listado de devoluciones a bodega. */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DevolucionBodegaDto {
	private Long idDevolucion;
	private LocalDateTime fechaRegistro;
	private LocalDateTime fechaResolucion;
	private EstadoDespachoBodegaEnum estado;
	@JsonSerialize(using = SoloAdministradorSerializer.class)
	private BigDecimal total;
	private String entregadoPor;
	private String motivo;
	private Integer idSucursalOrigen;
	private String sucursalOrigen;
	private Integer idBodega;
	private String bodega;
	private String usuarioRegistra;
	private String usuarioResuelve;
	private Integer totalLineas;
}
