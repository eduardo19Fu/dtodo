package xyz.pangosoft.dtodo.service.impl;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ClienteServiceImplTest {

    @Test
    void separaLosTerminosSinImponerUnaFraseContinua() {
        assertEquals(Arrays.asList("consejo", "el", "porvenir"),
                ClienteServiceImpl.obtenerTerminosBusqueda("  Consejo   El Porvenir  "));

        assertEquals(
                new HashSet<>(ClienteServiceImpl.obtenerTerminosBusqueda("Consejo El Porvenir")),
                new HashSet<>(ClienteServiceImpl.obtenerTerminosBusqueda("Porvenir Consejo El"))
        );
    }

    @Test
    void eliminaTerminosDuplicados() {
        assertEquals(Arrays.asList("consejo", "porvenir"),
                ClienteServiceImpl.obtenerTerminosBusqueda("Consejo CONSEJO Porvenir"));
    }

    @Test
    void aceptaFiltroVacioONulo() {
        assertEquals(Collections.emptyList(), ClienteServiceImpl.obtenerTerminosBusqueda("   "));
        assertEquals(Collections.emptyList(), ClienteServiceImpl.obtenerTerminosBusqueda(null));
    }
}
