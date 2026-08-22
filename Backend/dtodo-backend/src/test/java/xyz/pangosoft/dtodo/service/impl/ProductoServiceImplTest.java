package xyz.pangosoft.dtodo.service.impl;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProductoServiceImplTest {

    @Test
    void separaLosTerminosSinImponerUnaFraseContinua() {
        assertEquals(Arrays.asList("eclipse", "resma"),
                ProductoServiceImpl.obtenerTerminosBusqueda("  Eclipse   resma  "));

        assertEquals(
                new HashSet<>(ProductoServiceImpl.obtenerTerminosBusqueda("Eclipse resma")),
                new HashSet<>(ProductoServiceImpl.obtenerTerminosBusqueda("resma Eclipse"))
        );
    }

    @Test
    void eliminaTerminosDuplicados() {
        assertEquals(Arrays.asList("resma", "eclipse"),
                ProductoServiceImpl.obtenerTerminosBusqueda("Resma RESMA Eclipse"));
    }

    @Test
    void aceptaFiltroVacioONulo() {
        assertEquals(Collections.emptyList(), ProductoServiceImpl.obtenerTerminosBusqueda("   "));
        assertEquals(Collections.emptyList(), ProductoServiceImpl.obtenerTerminosBusqueda(null));
    }
}
