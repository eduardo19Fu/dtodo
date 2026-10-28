package xyz.pangosoft.dtodo.dto;

import java.time.LocalDateTime;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Un hecho en la línea de tiempo de un pedido: quién hizo qué y cuándo. Junta las etapas, el historial de auditoría, las
 * entregas, las facturas y los recibos ligados al pedido. No lleva importes: lo puede ver quien opera el pedido sin ver dinero.
 */
@Data
@NoArgsConstructor
public class HistorialEventoDto {

    /** Qué clase de hecho es; sirve para el icono y el color. */
    public enum Categoria { CREACION, EDICION, AUTORIZACION, ETAPA, ENTREGA, DEVOLUCION, FACTURA, RECIBO, ANULACION }

    private LocalDateTime fecha;
    private Categoria categoria;
    private String titulo;
    /** Nombre del usuario que lo hizo; puede ser nulo. */
    private String usuario;
    /** Nombre de quien lo hizo cuando no tiene usuario (por ejemplo, el personal que rota en bodega). */
    private String responsable;
    private String motivo;
    /** Texto largo opcional (por ejemplo, el antes y el después de una edición). */
    private String detalle;

    public HistorialEventoDto(LocalDateTime fecha, Categoria categoria, String titulo, String usuario, String responsable,
            String motivo, String detalle) {
        this.fecha = fecha;
        this.categoria = categoria;
        this.titulo = titulo;
        this.usuario = usuario;
        this.responsable = responsable;
        this.motivo = motivo;
        this.detalle = detalle;
    }
}
