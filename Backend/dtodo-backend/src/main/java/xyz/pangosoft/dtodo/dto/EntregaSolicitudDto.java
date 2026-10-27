package xyz.pangosoft.dtodo.dto;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import lombok.Data;
import lombok.NoArgsConstructor;

/** Lo que se envía para registrar una entrega. Los usuarios y las fechas de registro los pone el servidor. */
@Data
@NoArgsConstructor
public class EntregaSolicitudDto {

    /** Sucursal de la que se descuenta el inventario. */
    private Integer idSucursal;
    /** Fecha real de la entrega: no futura y hasta 7 días atrás; hoy si no viene. */
    private LocalDate fechaEntrega;
    /** Nombre de quien entregó, si no es el usuario que registra (opcional). */
    private String entregadoPor;
    /** Marcado: lo entregado ya se había descontado del inventario a mano, así que la entrega no lo descuenta. */
    private boolean descontadoAMano;
    private String observaciones;
    /**
     * Solo el administrador: entrega un pedido de contado aunque no esté cobrado. Queda anotado en las etapas del pedido.
     */
    private boolean entregarSinPago;
    private List<Linea> lineas = new ArrayList<>();

    @Data
    @NoArgsConstructor
    public static class Linea {
        private Long idDetalle;
        private Integer cantidad;
    }
}
