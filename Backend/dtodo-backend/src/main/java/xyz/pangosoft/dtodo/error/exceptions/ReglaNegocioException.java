package xyz.pangosoft.dtodo.error.exceptions;

/**
 * Se lanza cuando una operación válida en formato incumple una regla de negocio que el usuario puede
 * corregir sin salir de la pantalla (por ejemplo, facturar una proforma que ya tiene factura o usar un
 * NIT inválido).
 *
 * <p>Se traduce a {@code 422 Unprocessable Entity}. No se usa 401 ni 403 de forma intencional: el
 * interceptor del frontend cierra la sesión ante un 401 y redirige al inicio ante un 403, y en estos
 * casos el token sigue siendo válido.</p>
 */
public class ReglaNegocioException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ReglaNegocioException(String message) {
        super(message);
    }
}
