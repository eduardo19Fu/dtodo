package xyz.pangosoft.dtodo.dto;

import java.math.BigDecimal;

import lombok.Data;
import lombok.NoArgsConstructor;

/** Lo que un recibo aplica a un pedido: lo que se manda al emitir un recibo para varios pedidos y lo que se muestra después. */
@Data
@NoArgsConstructor
public class PartidaReciboDto {
    private Long idProforma;
    /** Solo al mostrar: el número del pedido. */
    private String noProforma;
    private BigDecimal monto;
}
