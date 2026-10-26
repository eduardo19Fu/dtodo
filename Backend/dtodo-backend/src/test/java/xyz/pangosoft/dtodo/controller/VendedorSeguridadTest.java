package xyz.pangosoft.dtodo.controller;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.annotation.Secured;

/**
 * Fija qué endpoints ve el vendedor. Si alguien amplía o recorta esta lista sin querer, una prueba lo avisa:
 * el rol de vendedor solo debe llegar a lo que necesita para sus clientes, proformas y facturas.
 */
class VendedorSeguridadTest {

    private static final String VENDEDOR = "ROLE_VENDEDOR";

    private static List<String> roles(Class<?> controlador, String metodo) {
        List<Method> metodos = Arrays.stream(controlador.getDeclaredMethods())
                .filter(m -> m.getName().equals(metodo)).collect(Collectors.toList());
        assertFalse(metodos.isEmpty(), "No existe " + controlador.getSimpleName() + "." + metodo);
        return metodos.stream().flatMap(m -> {
            Secured secured = m.getAnnotation(Secured.class);
            assertTrue(secured != null, controlador.getSimpleName() + "." + metodo + " no tiene @Secured");
            return Arrays.stream(secured.value());
        }).collect(Collectors.toList());
    }

    private static void permiteAlVendedor(Class<?> controlador, String... metodos) {
        for (String metodo : metodos) {
            assertTrue(roles(controlador, metodo).contains(VENDEDOR),
                    controlador.getSimpleName() + "." + metodo + " debería permitir al vendedor");
        }
    }

    private static void niegaAlVendedor(Class<?> controlador, String... metodos) {
        for (String metodo : metodos) {
            assertFalse(roles(controlador, metodo).contains(VENDEDOR),
                    controlador.getSimpleName() + "." + metodo + " no debería permitir al vendedor");
        }
    }

    @Test
    void elVendedorTrabajaConSusProformas() {
        permiteAlVendedor(ProformaApiController.class, "getListadoDto", "searchListadoDto", "getUltimasListadoDto",
                "cantidadProformas", "getDetalleDto", "findProforma", "create", "udpate");
    }

    @Test
    void elVendedorNoVeLasProformasDeLosDemasNiLasEliminaNiLasExporta() {
        niegaAlVendedor(ProformaApiController.class, "index", "delete", "getOrigenNotaDto", "findByNoProforma",
                "getProformasPorFecha", "getProformasPorFechaSp", "usuariosExportacion", "generarProformasExcel");
    }

    @Test
    void elVendedorRegistraYCorrigeClientesPeroNoLosElimina() {
        permiteAlVendedor(ClienteApiController.class, "create", "update");
        niegaAlVendedor(ClienteApiController.class, "delete", "cantidadNitInvalidos");
    }

    @Test
    void elVendedorSoloEmiteFacturasNoLasAnula() {
        permiteAlVendedor(FacturaApiController.class, "createV2");
        niegaAlVendedor(FacturaApiController.class, "cancelV2");
    }

    @Test
    void elVendedorConsultaSuUsuarioSuCorrelativoYLosProductos() {
        permiteAlVendedor(UsuarioApiController.class, "findById");
        permiteAlVendedor(CorrelativoApiController.class, "findByUsuario");
        permiteAlVendedor(ProductoApiController.class, "findById", "findByCodigo");
        niegaAlVendedor(ProductoApiController.class, "delete");
    }

    @Test
    void lasRutasSonDelAdministradorYElVendedorSoloConsultaLasSuyas() {
        permiteAlVendedor(RutaApiController.class, "mias");
        niegaAlVendedor(RutaApiController.class, "create", "update", "listado", "getById", "clientes", "activas");
        assertTrue(roles(RutaApiController.class, "mias").size() == 1, "mias es solo del vendedor");
    }
}
