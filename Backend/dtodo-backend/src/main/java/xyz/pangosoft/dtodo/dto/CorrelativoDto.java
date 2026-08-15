package xyz.pangosoft.dtodo.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CorrelativoDto {

	private Long idCorrelativo;
	private Long correlativoInicial;
	private Long correlativoFinal;
	private Long correlativoActual;
	private String serie;
	private LocalDateTime fechaCreacion;
	private String usuario;
	private Integer idEstado;
	private String estado;
}
