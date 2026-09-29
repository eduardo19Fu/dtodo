package xyz.pangosoft.dtodo.config;

import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("context-test")
class VerificadorEsquemaTest {

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Test
    void elMapeoIncluyeColumnasPropiasForaneasYDeColecciones() {
        Map<String, Set<String>> esperado = VerificadorEsquema.columnasEsperadas(entityManagerFactory);

        assertTrue(esperado.get("facturas").containsAll(Arrays.asList("id_factura", "id_proforma_origen", "id_cliente")));
        // FK de un @OneToMany unidireccional: vive en la tabla hija
        assertTrue(esperado.get("facturas_detalle").contains("id_factura"));
    }

    @Test
    void reportaTablasYColumnasFaltantes() {
        Map<String, Set<String>> esperado = new TreeMap<>();
        esperado.put("facturas", new TreeSet<>(Arrays.asList("id_factura", "id_proforma_origen")));
        esperado.put("despachos_nota", new TreeSet<>(Collections.singletonList("id_despacho")));
        Map<String, Set<String>> existente = new TreeMap<>();
        existente.put("facturas", new TreeSet<>(Collections.singletonList("id_factura")));

        List<String> faltantes = VerificadorEsquema.faltantes(esperado, existente);

        assertEquals(Arrays.asList("tabla despachos_nota", "columna facturas.id_proforma_origen"), faltantes);
    }

    @Test
    void noReportaNadaCuandoElEsquemaEstaCompleto() {
        Map<String, Set<String>> esperado = new TreeMap<>();
        esperado.put("facturas", new TreeSet<>(Collections.singletonList("id_factura")));

        assertTrue(VerificadorEsquema.faltantes(esperado, esperado).isEmpty());
    }

    @Test
    void normalizaComillasEsquemaYMayusculas() {
        assertEquals("facturas", VerificadorEsquema.normalizar("`prstd_db`.`FACTURAS`"));
    }
}
