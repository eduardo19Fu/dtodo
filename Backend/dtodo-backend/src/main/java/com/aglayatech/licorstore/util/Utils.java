package com.aglayatech.licorstore.util;

import lombok.NoArgsConstructor;

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
}
