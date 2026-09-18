package xyz.pangosoft.dtodo.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import xyz.pangosoft.dtodo.model.enums.EstadoCompraEnum;
import xyz.pangosoft.dtodo.model.enums.TipoComprobanteCompraEnum;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CompraDto {
	private Long idCompra;
	private LocalDate fechaCompra;
	private LocalDateTime fechaRegistro;
	private String noComprobante;
	private TipoComprobanteCompraEnum tipoComprobante;
	private BigDecimal total;
	private BigDecimal costoEnvio;
	private EstadoCompraEnum estado;
	private String proveedor;
	private String sucursal;
	private String usuario;
	private String registradoPor;
	private String observaciones;
}
