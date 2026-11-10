package xyz.pangosoft.dtodo.model.enums;

/**
 * Seguimiento de un cheque recibido de un cliente: RECIBIDO (en custodia), DEPOSITADO (llevado al banco), COBRADO (el banco
 * lo pagó) o RECHAZADO (el banco no lo pagó: anula el recibo). COBRADO y RECHAZADO son finales.
 */
public enum EstadoChequeEnum {
    RECIBIDO, DEPOSITADO, COBRADO, RECHAZADO;

    /** RECIBIDO puede pasar a DEPOSITADO o RECHAZADO; DEPOSITADO a COBRADO o RECHAZADO. */
    public boolean puedePasarA(EstadoChequeEnum destino) {
        switch (this) {
            case RECIBIDO:
                return destino == DEPOSITADO || destino == RECHAZADO;
            case DEPOSITADO:
                return destino == COBRADO || destino == RECHAZADO;
            default:
                return false;
        }
    }
}
