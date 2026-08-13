package xyz.pangosoft.dtodo.error.exceptions;

/**
 * Indica que se intentó crear un Correlativo para un usuario que ya cuenta con
 * un Correlativo activo.
 *
 * <p>Se mapea a una respuesta HTTP {@code 409 Conflict} por el handler global
 * de excepciones.</p>
 */
public class DuplicateCorrelativoException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public DuplicateCorrelativoException(String message) {
        super(message);
    }

    public DuplicateCorrelativoException(String message, Throwable cause) {
        super(message, cause);
    }
}
