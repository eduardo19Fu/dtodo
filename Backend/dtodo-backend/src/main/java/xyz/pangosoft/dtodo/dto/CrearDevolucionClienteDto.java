package xyz.pangosoft.dtodo.dto;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Lo que se manda para registrar la devolución de un cliente. El administrador la registra y queda aprobada al registrarla.
 */
@Data
@NoArgsConstructor
public class CrearDevolucionClienteDto {
    private Long idProforma;
    /** Fecha de la devolución; hoy si no se manda. No puede ser futura. */
    private LocalDate fecha;
    private String motivo;
    /** Sucursal que recibe lo que reingresa; la del pedido si no se manda. Solo hace falta si hay líneas de reingreso. */
    private Integer idSucursal;
    /** Obligatoria cuando el pedido tiene una factura vigente: la nota de crédito a la que se liga. */
    private Long idNotaCredito;
    private List<Linea> lineas = new ArrayList<>();

    @Data
    @NoArgsConstructor
    public static class Linea {
        private Long idDetalle;
        private Integer cantidad;
        /** REINGRESO o BAJA. */
        private String destino;
    }
}
