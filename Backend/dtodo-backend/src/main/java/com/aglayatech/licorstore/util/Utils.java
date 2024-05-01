package com.aglayatech.licorstore.util;

import lombok.NoArgsConstructor;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Esta es una clase que contiene distintos métodos de manejo de strings, conversión de tipos y demás funcionalidades
 * que pueden resultar utiles a lo largo del proyecto.
 */

@NoArgsConstructor
public class Utils {

    public static Date stringToDate(String date) throws ParseException {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        return format.parse(date);
    }
}
