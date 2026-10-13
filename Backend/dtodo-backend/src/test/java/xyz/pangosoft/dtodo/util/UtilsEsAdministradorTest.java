package xyz.pangosoft.dtodo.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UtilsEsAdministradorTest {

    @AfterEach
    void limpiarSesion() {
        SecurityContextHolder.clearContext();
    }

    private void iniciarSesion(String... roles) {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("usuario", "x", roles));
    }

    @Test
    void unAdministradorEsAdministradorAunqueTengaOtrosRoles() {
        iniciarSesion("ROLE_BODEGA", "ROLE_ADMIN");

        assertTrue(Utils.esAdministrador());
        assertTrue(Utils.esAdministrador(SecurityContextHolder.getContext().getAuthentication()));
    }

    @Test
    void ningunOtroRolEsAdministrador() {
        for (String rol : new String[] { "ROLE_BODEGA", "ROLE_COBRADOR", "ROLE_INVENTARIO" }) {
            iniciarSesion(rol);

            assertFalse(Utils.esAdministrador(), rol);
        }
    }

    @Test
    void sinSesionONiAutenticacionNoEsAdministrador() {
        assertFalse(Utils.esAdministrador());
        assertFalse(Utils.esAdministrador(null));
    }
}
