package xyz.pangosoft.dtodo.dto;

import java.time.LocalDate;

import lombok.Data;
import lombok.NoArgsConstructor;

/** Los datos del cheque que se mandan al emitir un recibo pagado con cheque. */
@Data
@NoArgsConstructor
public class DatosChequeDto {
    private String banco;
    private String numeroCheque;
    /** Opcional: quien firma el cheque, si no es el cliente. */
    private String girador;
    /** La fecha escrita en el cheque. Si es posterior a la del pago, queda como postfechado. */
    private LocalDate fechaCheque;
    private String notas;
}
