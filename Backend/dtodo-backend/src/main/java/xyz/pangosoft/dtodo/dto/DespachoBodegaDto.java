package xyz.pangosoft.dtodo.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import xyz.pangosoft.dtodo.model.enums.EstadoDespachoBodegaEnum;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import xyz.pangosoft.dtodo.util.SoloAdministradorSerializer;

/** Fila del listado de despachos de bodega. */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DespachoBodegaDto {
	private Long idDespacho;
	private LocalDateTime fechaRegistro;
	private LocalDateTime fechaResolucion;
	private EstadoDespachoBodegaEnum estado;
	@JsonSerialize(using = SoloAdministradorSerializer.class)
	private BigDecimal total;
	private String recibidoPor;
	private String observaciones;
	private Integer idBodega;
	private String bodega;
	private Integer idSucursalDestino;
	private String sucursalDestino;
	private Integer idBodegaDestino;
	private String bodegaDestino;
	private String usuarioDespacha;
	private String usuarioResuelve;
	private Integer totalLineas;
}
