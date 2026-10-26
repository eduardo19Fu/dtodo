package xyz.pangosoft.dtodo.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import xyz.pangosoft.dtodo.model.enums.EstadoPedidoEnum;

/** Un paso del pedido, listo para mostrarse. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PedidoEtapaDto {
    private Long idEtapa;
    private EstadoPedidoEnum etapa;
    /** Nombre del usuario que lo registró. */
    private String usuario;
    /** Nombre de quien lo hizo cuando no tiene usuario (personal que rota); puede ser nulo. */
    private String responsable;
    /** Se llena al retroceder el pedido o al reiniciarlo porque se editó la proforma. */
    private String motivo;
    private LocalDateTime fecha;
}
