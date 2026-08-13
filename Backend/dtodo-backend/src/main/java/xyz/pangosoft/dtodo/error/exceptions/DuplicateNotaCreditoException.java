package xyz.pangosoft.dtodo.error.exceptions;

/**
 * Indica que se intentó crear una Nota de Crédito sobre un documento origen
 * (Factura o Proforma) que ya posee una Nota de Crédito previamente registrada.
 *
 * <p>Se mapea a una respuesta HTTP {@code 409 Conflict} por el handler global
 * de excepciones.</p>
 */
public class DuplicateNotaCreditoException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public DuplicateNotaCreditoException(String message) {
        super(message);
    }

    public DuplicateNotaCreditoException(String message, Throwable cause) {
        super(message, cause);
    }
}
