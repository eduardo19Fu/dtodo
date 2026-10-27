package xyz.pangosoft.dtodo.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import lombok.Data;
import lombok.NoArgsConstructor;

/** Una entrega de un pedido, lista para mostrarse. */
@Data
@NoArgsConstructor
public class EntregaDto {

    private Long idEntrega;
    private LocalDate fechaEntrega;
    private LocalDateTime fechaRegistro;
    /** Quien la registró. */
    private String usuario;
    private String entregadoPor;
    private Integer idSucursal;
    private String sucursal;
    private boolean descontadoAMano;
    private String observaciones;
    private boolean anulada;
    private LocalDateTime fechaAnulacion;
    private String motivoAnulacion;
    private List<Linea> lineas = new ArrayList<>();

    @Data
    @NoArgsConstructor
    public static class Linea {
        private Long idDetalle;
        private String producto;
        private Integer cantidad;
    }
}
