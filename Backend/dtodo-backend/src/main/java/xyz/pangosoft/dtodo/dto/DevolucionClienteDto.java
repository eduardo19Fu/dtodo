package xyz.pangosoft.dtodo.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import lombok.Data;
import lombok.NoArgsConstructor;

/** Una devolución de cliente, en la lista (sin líneas) y en el detalle. */
@Data
@NoArgsConstructor
public class DevolucionClienteDto {
    private Long idDevolucion;
    private Long idProforma;
    private String noProforma;
    private Integer idCliente;
    private String cliente;
    private Integer idSucursal;
    private String sucursal;
    private Long idNotaCredito;
    private LocalDate fecha;
    private String motivo;
    private BigDecimal total;
    private String usuario;
    private LocalDateTime fechaRegistro;
    private boolean anulada;
    private LocalDateTime fechaAnulacion;
    private String usuarioAnulacion;
    private String motivoAnulacion;
    private List<Linea> lineas = new ArrayList<>();

    @Data
    @NoArgsConstructor
    public static class Linea {
        private Long idDetalle;
        private String codigo;
        private String producto;
        private Integer cantidad;
        private String destino;
        private BigDecimal valor;
    }
}
