package xyz.pangosoft.dtodo.model.enums;

/** Cómo pagó el cliente un recibo de caja. Depósito y transferencia piden el número de referencia. */
public enum FormaPagoReciboEnum {
    EFECTIVO(false),
    DEPOSITO(true),
    TRANSFERENCIA(true);

    private final boolean pideReferencia;

    FormaPagoReciboEnum(boolean pideReferencia) {
        this.pideReferencia = pideReferencia;
    }

    public boolean pideReferencia() {
        return pideReferencia;
    }
}
