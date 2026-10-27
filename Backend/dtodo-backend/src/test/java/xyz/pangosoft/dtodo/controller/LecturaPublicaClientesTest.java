package xyz.pangosoft.dtodo.controller;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.annotation.Secured;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Los datos de los clientes (nombre, NIT, dirección, teléfono, ruta y crédito) no se leen sin iniciar sesión, y solo
 * los roles que trabajan con clientes los leen. Antes tres lecturas estaban abiertas al público.
 */
@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@AutoConfigureMockMvc
@ActiveProfiles("context-test")
class LecturaPublicaClientesTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void sinTokenNingunaLecturaDeClientesResponde() throws Exception {
        for (String ruta : new String[] { "/api/clientes", "/api/clientes/1", "/api/clientes/page/0", "/api/clientes/nit/4514627-6",
                "/api/clientes/listado", "/api/clientes/codigo/55551234", "/api/clientes/cantidad-clientes" }) {
            mockMvc.perform(get(ruta)).andExpect(status().isUnauthorized());
        }
    }

    @Test
    void sinTokenTampocoSeLeenFacturasNotasDeCreditoNiSeImprimeUnaProforma() throws Exception {
        for (String ruta : new String[] { "/api/facturas/page/0", "/api/facturas", "/api/notas-credito",
                "/api/proformas/generate/1", "/api/usuarios/page/0", "/api/usuarios/cajero" }) {
            mockMvc.perform(get(ruta)).andExpect(status().isUnauthorized());
        }
    }

    @Test
    void lasLecturasQueSiSonPublicasSiguenSiendolo() throws Exception {
        // El catálogo de productos y sus imágenes no cambia: no son datos de clientes.
        mockMvc.perform(get("/api/marcas")).andExpect(status().is(org.hamcrest.Matchers.not(401)));
    }

    @Test
    void todaLecturaDeClientesDeclaraSusRolesYNingunaIncluyeABodegaNiAlRepartidor() {
        List<Method> lecturas = Arrays.stream(ClienteApiController.class.getDeclaredMethods())
                .filter(metodo -> metodo.isAnnotationPresent(GetMapping.class)).collect(Collectors.toList());
        assertFalse(lecturas.isEmpty());
        for (Method lectura : lecturas) {
            Secured secured = lectura.getAnnotation(Secured.class);
            assertTrue(secured != null, lectura.getName() + " no tiene @Secured");
            List<String> roles = Arrays.asList(secured.value());
            assertFalse(roles.contains("ROLE_BODEGA"), lectura.getName() + " no debe permitir a bodega");
            assertFalse(roles.contains("ROLE_REPARTIDOR"), lectura.getName() + " no debe permitir al repartidor");
        }
    }
}
