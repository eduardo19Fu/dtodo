package xyz.pangosoft.dtodo.util;

import lombok.NoArgsConstructor;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Esta es una clase que contiene distintos métodos de manejo de strings, conversión de tipos y demás funcionalidades
 * que pueden resultar utiles a lo largo del proyecto.
 */

@NoArgsConstructor
public class Utils {

    /**
     * Método que devuelve configura una fecha en determinado formato y la devuelve
     * @param date Objeto tipo String que representa la fecha que se desea convertir
     * @return La fecha una vez convertida en el formato deseado.
     * @throws ParseException
     *
     * */
    public static Date stringToDate(String date) throws ParseException {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        return format.parse(date);
    }

    /**
     * Recibe una fecha determinada y la devuelve como un String con el formato especificado y la zona horaria
     * deseada.
     * @param date Objeto de tipo java.util.Date que representa la fecha que se desea configurar
     * @return La fecha como un String una vez configurada
     *
     * */
    public static String setDateFormat(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'-06:00'", Locale.getDefault());
        sdf.setTimeZone(TimeZone.getTimeZone("America/Guatemala"));
        return sdf.format(date);
    }

    public static String fechaCertificacionTransformada(String fechaCertificacionSat) {
        return fechaCertificacionSat.trim().replace("T", "'T'").replace("-06:00", "'-06:00'");
    }

    public static SimpleDateFormat getDateFormat(String dateString) {
        SimpleDateFormat format = new SimpleDateFormat(dateString);
        format.setTimeZone(TimeZone.getTimeZone("America/Guatemala"));
        return format;
    }

    /**
     *
     * */
    public static String formatearNitParaCertificacion(String nit) {
        if (nit.equals("C/F")) {
            return (nit.replace("/", "").trim());
        } else if(nit.contains("-")) {
            return (nit.replace("-", "").trim());
        } else {
            return (nit.trim());
        }
    }

    /**
     * Método que lleva a cabo la generación del numero de Proforma
     * */
    public static String generarNoProforma() {
        String noProforma = "";
        Long numerico = (long) (Math.random()*1000000000+1);
        noProforma = numerico + "P";
        return noProforma;
    }

    /**
     * Obtiene el id del usuario autenticado desde el claim {@code id_usuario} del token JWT, para que las
     * operaciones que auditan al responsable no dependan de un identificador enviado por el cliente.
     * @param jwt Token de la sesión actual
     * @return El id del usuario autenticado
     * @throws AccessDeniedException si el token no trae un id de usuario válido
     * */
    public static Integer obtenerIdUsuario(Jwt jwt) {
        String claim = jwt == null ? null : jwt.getClaimAsString("id_usuario");
        if (claim == null || claim.isBlank()) {
            throw new AccessDeniedException("No se pudo identificar al usuario de la sesión.");
        }
        try {
            return Integer.valueOf(claim);
        } catch (java.lang.NumberFormatException exception) {
            throw new AccessDeniedException("El usuario de la sesión no es válido.");
        }
    }
}
