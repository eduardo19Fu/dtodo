package xyz.pangosoft.dtodo.dto;

import java.math.BigDecimal;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import xyz.pangosoft.dtodo.model.enums.TipoDescuentoEnum;

/** Una forma de descuento que se le puede ofrecer al usuario para una línea de proforma. */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OpcionDescuentoDto {

    private TipoDescuentoEnum tipo;
    private String etiqueta;

    /** Porcentaje que aplica; nulo cuando hay varios niveles que elegir o cuando es libre. */
    private BigDecimal porcentaje;

    /** Niveles entre los que elegir (categoría Volumen); vacío en los demás tipos. */
    private List<BigDecimal> niveles;

    /** Cantidad desde la que aplica (ciento, fardo); nulo si no depende de la cantidad. */
    private Integer cantidadMinima;

    /** {@code false} cuando la cantidad pedida no llega al mínimo: se avisa, pero no se bloquea. */
    private boolean alcanzaMinimo;

    /** Precio unitario resultante, solo para el precio mayorista fijo. */
    private BigDecimal precio;
}
