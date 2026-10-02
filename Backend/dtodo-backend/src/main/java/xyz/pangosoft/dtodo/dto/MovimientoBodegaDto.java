package xyz.pangosoft.dtodo.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import xyz.pangosoft.dtodo.model.enums.TipoMovimientoBodegaEnum;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MovimientoBodegaDto {
	private Long idMovimiento;
	private LocalDateTime fechaMovimiento;
	private TipoMovimientoBodegaEnum tipoMovimiento;
	private Integer cantidad;
	private Integer stockInicial;
	private Integer stockFinal;
	private String motivo;
	private String tipoDocumentoOrigen;
	private Long idDocumentoOrigen;
	private String codProducto;
	private String producto;
	private String usuario;
}
