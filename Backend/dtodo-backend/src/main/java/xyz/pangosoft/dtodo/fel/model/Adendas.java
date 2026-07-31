package xyz.pangosoft.dtodo.fel.model;

import java.util.Hashtable;

/**
 * Adenda genérica del DTE (nodo {@code dte:Adenda}): pares clave/valor que se
 * serializan como etiquetas en minúsculas ordenadas alfabéticamente.
 * Reemplazo de {@code com.fel.validaciones.documento.Adendas}.
 */
public class Adendas {

    private final Hashtable<String, String> adenda = new Hashtable<>();

    public Hashtable<String, String> getAdenda() {
        return adenda;
    }

    public void setAdenda(String clave, String valor) {
        this.adenda.put(clave, valor);
    }
}
