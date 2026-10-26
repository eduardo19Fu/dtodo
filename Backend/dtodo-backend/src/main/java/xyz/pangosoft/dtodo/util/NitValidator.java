package xyz.pangosoft.dtodo.util;

/**
 * Valida el NIT guatemalteco con el algoritmo de la SAT (módulo 11 sobre el dígito verificador).
 *
 * <p>Sirve para detectar antes de certificar en FEL los valores que no son un NIT, por ejemplo un teléfono
 * guardado en el campo NIT: la certificadora los rechaza y la venta se revierte. {@code CF} y {@code C/F}
 * (consumidor final) se aceptan siempre.</p>
 */
public final class NitValidator {

    private static final int MAX_DIGITOS_CUERPO = 11;

    private NitValidator() {
    }

    /**
     * @return {@code true} si el valor es consumidor final ({@code CF} o {@code C/F}, sin importar mayúsculas)
     */
    public static boolean esConsumidorFinal(String nit) {
        if (nit == null) {
            return false;
        }
        String normalizado = nit.trim().toUpperCase().replace("-", "").replace(" ", "");
        return normalizado.equals("CF") || normalizado.equals("C/F");
    }

    /**
     * @return {@code true} si es consumidor final o un NIT con dígito verificador correcto, con o sin guion
     */
    public static boolean esValido(String nit) {
        if (nit == null || nit.trim().isEmpty()) {
            return false;
        }
        if (esConsumidorFinal(nit)) {
            return true;
        }

        String normalizado = nit.trim().toUpperCase().replace("-", "").replace(" ", "");
        if (!normalizado.matches("\\d{1," + MAX_DIGITOS_CUERPO + "}[0-9K]")) {
            return false;
        }

        String cuerpo = normalizado.substring(0, normalizado.length() - 1);
        char digitoVerificador = normalizado.charAt(normalizado.length() - 1);

        int factor = cuerpo.length() + 1;
        int suma = 0;
        for (char digito : cuerpo.toCharArray()) {
            suma += (digito - '0') * factor;
            factor--;
        }

        int residuo = (11 - (suma % 11)) % 11;
        char esperado = residuo == 10 ? 'K' : (char) ('0' + residuo);
        return digitoVerificador == esperado;
    }
}
