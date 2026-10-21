package xyz.pangosoft.dtodo.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class TerminosBusquedaTest {

    private static final String[] VACIO = { "", "", "", "", "", "" };

    @Test
    void unTextoNuloOEnBlancoNoAgregaCondiciones() {
        assertArrayEquals(VACIO, TerminosBusqueda.dividir(null));
        assertArrayEquals(VACIO, TerminosBusqueda.dividir("   "));
    }

    @Test
    void dividePorPalabrasEnMinusculasSinImportarLosEspaciosRepetidos() {
        assertArrayEquals(new String[] { "caja", "resma", "", "", "", "" }, TerminosBusqueda.dividir("  Caja   RESMA "));
    }

    @Test
    void elOrdenEscritoSeConservaPeroCadaTerminoSeBuscaPorSeparado() {
        assertArrayEquals(new String[] { "resma", "caja", "", "", "", "" }, TerminosBusqueda.dividir("resma caja"));
    }

    @Test
    void lasPalabrasSobrantesSeUnenALaUltima() {
        String[] t = TerminosBusqueda.dividir("a b c d e f g h");

        assertEquals(TerminosBusqueda.MAXIMO, t.length);
        assertEquals("f g h", t[5]);
    }
}
