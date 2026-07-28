package xyz.pangosoft.dtodo.error.exceptions;

/**
 * Se lanza cuando una operación exige reautenticación (confirmar la contraseña
 * del usuario en sesión) y la contraseña enviada no coincide con la almacenada.
 *
 * <p>No se traduce a 401 ni a 403 de forma intencional: el interceptor del
 * frontend cierra la sesión ante un 401 y redirige al inicio ante un 403, y en
 * este caso el token sigue siendo válido — lo único inválido es la contraseña
 * escrita en el modal de autorización.</p>
 */
public class InvalidPasswordException extends RuntimeException {

    public InvalidPasswordException(String message) {
        super(message);
    }
}
