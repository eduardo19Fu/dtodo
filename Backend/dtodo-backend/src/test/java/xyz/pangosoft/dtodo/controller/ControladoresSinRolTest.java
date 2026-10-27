package xyz.pangosoft.dtodo.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Todo endpoint declara los roles que lo usan. Los únicos que no lo hacen son las lecturas del catálogo (productos,
 * marcas, tipos de producto, roles y rangos de correlativos), que son públicas a propósito. Un endpoint nuevo sin
 * {@code @Secured} cualquier usuario con sesión lo alcanzaría —incluido el repartidor—, así que falla esta prueba hasta
 * que se le ponga rol o se agregue aquí con una razón.
 */
class ControladoresSinRolTest {

    private static final Set<String> CATALOGO_PUBLICO = new TreeSet<>(Arrays.asList(
            "CorrelativoApiController.index",
            "MarcaProductoApiController.index", "MarcaProductoApiController.listado",
            "ProductoApiController.listado", "ProductoApiController.getAllDto", "ProductoApiController.getPageDto",
            "ProductoApiController.searchProductosDto", "ProductoApiController.index", "ProductoApiController.findAll",
            "ProductoApiController.verImagen", "ProductoApiController.findByName",
            "RoleApiController.index",
            "TipoProductoApiController.index", "TipoProductoApiController.listado"));

    @Test
    void soloLasLecturasDelCatalogoQuedanSinRol() throws Exception {
        ClassPathScanningCandidateComponentProvider escaner = new ClassPathScanningCandidateComponentProvider(false);
        escaner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));

        Set<String> sinRol = new TreeSet<>();
        for (BeanDefinition definicion : escaner.findCandidateComponents("xyz.pangosoft.dtodo.controller")) {
            Class<?> controlador = Class.forName(definicion.getBeanClassName());
            boolean rolEnLaClase = controlador.isAnnotationPresent(Secured.class);
            for (Method metodo : controlador.getDeclaredMethods()) {
                boolean esEndpoint = metodo.isAnnotationPresent(GetMapping.class) || metodo.isAnnotationPresent(PostMapping.class)
                        || metodo.isAnnotationPresent(PutMapping.class) || metodo.isAnnotationPresent(DeleteMapping.class)
                        || metodo.isAnnotationPresent(PatchMapping.class) || metodo.isAnnotationPresent(RequestMapping.class);
                if (esEndpoint && !rolEnLaClase && !metodo.isAnnotationPresent(Secured.class)) {
                    sinRol.add(controlador.getSimpleName() + "." + metodo.getName());
                }
            }
        }

        assertEquals(CATALOGO_PUBLICO, sinRol, "Endpoints sin @Secured que no son del catálogo público");
    }
}
