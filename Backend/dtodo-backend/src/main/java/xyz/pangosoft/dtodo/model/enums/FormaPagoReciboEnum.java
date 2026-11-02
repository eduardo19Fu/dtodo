package xyz.pangosoft.dtodo.model.enums;

/** Cómo pagó el cliente un recibo de caja. Depósito, transferencia y cheque piden el número de referencia (boleta, transferencia o cheque). */
public enum FormaPagoReciboEnum {
    EFECTIVO(false),
    DEPOSITO(true),
    TRANSFERENCIA(true),
    CHEQUE(true);

    private final boolean pideReferencia;

    FormaPagoReciboEnum(boolean pideReferencia) {
        this.pideReferencia = pideReferencia;
    }

    public boolean pideReferencia() {
        return pideReferencia;
    }
}
