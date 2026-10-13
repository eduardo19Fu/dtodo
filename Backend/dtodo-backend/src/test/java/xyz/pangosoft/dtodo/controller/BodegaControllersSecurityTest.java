package xyz.pangosoft.dtodo.controller;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * El módulo de bodegas es exclusivo de ROLE_ADMIN y ROLE_BODEGA. Cada endpoint debe declarar su @Secured
 * explícitamente: un método sin anotación quedaría abierto a cualquier usuario autenticado.
 */
class BodegaControllersSecurityTest {

    private static final Set<String> ROLES_DEL_MODULO = Set.of("ROLE_ADMIN", "ROLE_BODEGA");

    private List<Method> endpoints(Class<?> controlador) {
        return Arrays.stream(controlador.getDeclaredMethods())
                .filter(metodo -> metodo.isAnnotationPresent(GetMapping.class)
                        || metodo.isAnnotationPresent(PostMapping.class)
                        || metodo.isAnnotationPresent(PutMapping.class)
                        || metodo.isAnnotationPresent(DeleteMapping.class))
                .collect(Collectors.toList());
    }

    private Set<String> roles(Method metodo) {
        Secured secured = metodo.getAnnotation(Secured.class);
        assertNotNull(secured, "El endpoint " + metodo.getName() + " no declara @Secured");
        return new TreeSet<>(Arrays.asList(secured.value()));
    }

    @Test
    void todosLosEndpointsDeBodegasDeclaranSeguridadYSoloPermitenRolesDelModulo() {
        for (Class<?> controlador : new Class<?>[] { BodegaApiController.class, DespachoBodegaApiController.class }) {
            List<Method> metodos = endpoints(controlador);
            assertFalse(metodos.isEmpty());
            for (Method metodo : metodos) {
                Set<String> roles = roles(metodo);
                assertTrue(ROLES_DEL_MODULO.containsAll(roles),
                        metodo.getName() + " permite roles fuera del módulo: " + roles);
            }
        }
    }

    @Test
    void aprobarUnDespachoEsExclusivoDeAdmin() throws Exception {
        Method aprobar = Arrays.stream(DespachoBodegaApiController.class.getDeclaredMethods())
                .filter(metodo -> metodo.getName().equals("aprobar")).findFirst().orElseThrow();

        assertEquals(Set.of("ROLE_ADMIN"), roles(aprobar));
    }

    @Test
    void reducirExistenciasYEliminarProductosDeUnaBodegaEsExclusivoDeAdmin() {
        List<Method> metodos = endpoints(BodegaApiController.class);
        for (String nombre : List.of("reducirExistencias", "eliminarProducto")) {
            Method ajuste = metodos.stream().filter(metodo -> metodo.getName().equals(nombre)).findFirst().orElseThrow();

            assertEquals(Set.of("ROLE_ADMIN"), roles(ajuste), nombre);
        }
    }

    @Test
    void agregarEImportarInventarioSigueAbiertoAlRolBodega() {
        List<Method> metodos = endpoints(BodegaApiController.class);
        for (String nombre : List.of("agregarProducto", "importarExcel", "clonarInventario")) {
            Method alta = metodos.stream().filter(metodo -> metodo.getName().equals(nombre)).findFirst().orElseThrow();

            assertEquals(ROLES_DEL_MODULO, roles(alta), nombre);
        }
    }

    @Test
    void elRestoDeLosEndpointsDeDespachosAdmiteAAdminYBodega() {
        for (Method metodo : endpoints(DespachoBodegaApiController.class)) {
            if (!metodo.getName().equals("aprobar")) {
                assertEquals(ROLES_DEL_MODULO, roles(metodo), metodo.getName());
            }
        }
    }

    @Test
    void losReportesDeBodegaAdmitenAAdminYBodegaYNoAInventario() {
        List<Method> reportesBodega = Arrays.stream(ReporteApiController.class.getDeclaredMethods())
                .filter(metodo -> metodo.getName().endsWith("Bodega") || metodo.getName().equals("bodegasSelector"))
                .collect(Collectors.toList());

        assertEquals(4, reportesBodega.size());
        for (Method metodo : reportesBodega) {
            assertEquals(ROLES_DEL_MODULO, roles(metodo), metodo.getName());
        }
    }
}
