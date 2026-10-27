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

    @Test
    void elVendedorConsultaLasOpcionesDeDescuentoPeroSoloElAdministradorLasConfigura() {
        permiteAlVendedor(DescuentoApiController.class, "opciones");
        niegaAlVendedor(DescuentoApiController.class, "categorias", "guardarCategoria", "productos", "producto",
                "guardarProducto");
        assertTrue(roles(DescuentoApiController.class, "guardarProducto").equals(Arrays.asList("ROLE_ADMIN")),
                "guardarProducto es solo del administrador");
    }

    @Test
    void elVendedorVeElHistorialDeSusProformasPeroLasAutorizacionesSonDelAdministrador() {
        permiteAlVendedor(ProformaApiController.class, "auditoria");
        niegaAlVendedor(AutorizacionProformaApiController.class, "pendientes", "vencidas", "aprobar", "rechazar",
                "autorizarVencida");
        assertTrue(roles(AutorizacionProformaApiController.class, "aprobar").equals(Arrays.asList("ROLE_ADMIN")),
                "aprobar es solo del administrador");
    }

    @Test
    void losReportesDeMayoristasSonSoloDelAdministrador() {
        niegaAlVendedor(ReporteMayoristasApiController.class, "clientesPorRuta", "clientesSinCompras", "proformasPorVendedor",
                "entregadoPorRutaYVendedor");
        for (String metodo : new String[] { "clientesPorRuta", "clientesSinCompras", "proformasPorVendedor", "entregadoPorRutaYVendedor" }) {
            assertTrue(roles(ReporteMayoristasApiController.class, metodo).equals(Arrays.asList("ROLE_ADMIN")),
                    metodo + " es solo del administrador");
        }
    }

    @Test
    void lasComisionesSonSoloDelAdministrador() {
        String[] metodos = { "metas", "guardarMetas", "copiarMetas", "avance", "liquidar", "deshacer" };
        niegaAlVendedor(ComisionApiController.class, metodos);
        for (String metodo : metodos) {
            assertTrue(roles(ComisionApiController.class, metodo).equals(Arrays.asList("ROLE_ADMIN")),
                    metodo + " es solo del administrador");
        }
    }

    @Test
    void elVendedorSoloVeSusPedidosYBodegaOperaLasEtapas() {
        permiteAlVendedor(PedidoApiController.class, "listar", "detalle");
        niegaAlVendedor(PedidoApiController.class, "avanzar", "retroceder", "picking", "empaque", "notaEnvio");
        assertTrue(roles(PedidoApiController.class, "avanzar").equals(Arrays.asList("ROLE_ADMIN", "ROLE_BODEGA")),
                "avanzar es de administrador y bodega");
        assertTrue(roles(PedidoApiController.class, "retroceder").equals(Arrays.asList("ROLE_ADMIN")),
                "retroceder es solo del administrador");
        for (String metodo : new String[] { "listar", "detalle", "avanzar", "picking", "empaque", "notaEnvio" }) {
            assertTrue(roles(PedidoApiController.class, metodo).contains("ROLE_BODEGA"), metodo + " debe permitir a bodega");
        }
    }

    @Test
    void laEntregaLaRegistranAdministradorBodegaYVendedorPeroAnularlaOCerrarEsDelAdministrador() {
        permiteAlVendedor(PedidoApiController.class, "registrarEntrega", "sucursales");
        niegaAlVendedor(PedidoApiController.class, "anularEntrega", "cerrarConLoEntregado");
        assertTrue(roles(PedidoApiController.class, "registrarEntrega").contains("ROLE_BODEGA"), "bodega registra entregas");
        assertTrue(roles(PedidoApiController.class, "anularEntrega").equals(Arrays.asList("ROLE_ADMIN")),
                "anularEntrega es solo del administrador");
        assertTrue(roles(PedidoApiController.class, "cerrarConLoEntregado").equals(Arrays.asList("ROLE_ADMIN")),
                "cerrarConLoEntregado es solo del administrador");
    }

    @Test
    void elRepartidorVeYEntregaSusPedidosPeroNadaMasDeLosPedidos() {
        for (String metodo : new String[] { "listar", "detalle", "registrarEntrega" }) {
            assertTrue(roles(PedidoApiController.class, metodo).contains("ROLE_REPARTIDOR"), metodo + " debe permitir al repartidor");
        }
        for (String metodo : new String[] { "avanzar", "retroceder", "picking", "empaque", "notaEnvio", "sucursales", "repartidores",
                "asignarRepartidor", "anularEntrega", "cerrarConLoEntregado" }) {
            assertFalse(roles(PedidoApiController.class, metodo).contains("ROLE_REPARTIDOR"), metodo + " no debe permitir al repartidor");
        }
        assertTrue(roles(PedidoApiController.class, "asignarRepartidor").equals(Arrays.asList("ROLE_ADMIN", "ROLE_BODEGA")));
        assertTrue(roles(PedidoApiController.class, "repartidores").equals(Arrays.asList("ROLE_ADMIN", "ROLE_BODEGA")));
    }

    @Test
    void elRepartidorNoLlegaANingunOtroControlador() throws Exception {
        for (Class<?> controlador : new Class<?>[] { ProformaApiController.class, FacturaApiController.class, ClienteApiController.class,
                ProductoApiController.class, RutaApiController.class, DescuentoApiController.class,
                AutorizacionProformaApiController.class, ReporteMayoristasApiController.class, ComisionApiController.class }) {
            for (Method metodo : controlador.getDeclaredMethods()) {
                Secured secured = metodo.getAnnotation(Secured.class);
                if (secured != null) {
                    assertFalse(Arrays.asList(secured.value()).contains("ROLE_REPARTIDOR"),
                            controlador.getSimpleName() + "." + metodo.getName());
                }
            }
        }
    }
}
