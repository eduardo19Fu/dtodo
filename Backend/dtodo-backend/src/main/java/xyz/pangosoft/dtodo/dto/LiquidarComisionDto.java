package xyz.pangosoft.dtodo.dto;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;
import lombok.NoArgsConstructor;

/** La decisión del administrador al liquidar un año: quiénes completaron el año. Quien no aparezca queda como que no. */
@Data
@NoArgsConstructor
public class LiquidarComisionDto {

    private List<Decision> decisiones = new ArrayList<>();

    @Data
    @NoArgsConstructor
    public static class Decision {
        private Integer idVendedor;
        private boolean completoAnio;
    }
}
