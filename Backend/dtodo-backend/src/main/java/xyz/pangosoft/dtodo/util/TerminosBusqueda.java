package xyz.pangosoft.dtodo.util;

import java.util.Locale;

/**
 * Divide un texto de búsqueda en palabras para que un listado encuentre los registros que contienen todas,
 * sin importar el orden en que se escribieron (igual que la búsqueda de productos).
 */
public final class TerminosBusqueda {

	/** Cantidad de palabras que distinguen las consultas con parámetros fijos; las sobrantes se unen a la última. */
	public static final int MAXIMO = 6;

	private TerminosBusqueda() {
	}

	/**
	 * Devuelve {@link #MAXIMO} términos en minúsculas; los que no se usan quedan como cadena vacía, que en las
	 * consultas significa "sin condición". Si hay más palabras que {@link #MAXIMO}, las últimas se unen con espacio.
	 */
	public static String[] dividir(String filtro) {
		String[] terminos = new String[MAXIMO];
		java.util.Arrays.fill(terminos, "");
		if (filtro == null || filtro.trim().isEmpty()) {
			return terminos;
		}
		String[] palabras = filtro.trim().toLowerCase(Locale.ROOT).split("\s+", MAXIMO);
		System.arraycopy(palabras, 0, terminos, 0, palabras.length);
		return terminos;
	}
}
