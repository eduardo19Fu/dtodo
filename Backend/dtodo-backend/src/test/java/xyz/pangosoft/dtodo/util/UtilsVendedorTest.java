package xyz.pangosoft.dtodo.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

class UtilsVendedorTest {

    @AfterEach
    void limpiarSesion() {
        SecurityContextHolder.clearContext();
    }

    private static Jwt token(String idUsuario) {
        Jwt.Builder builder = Jwt.withTokenValue("token").header("alg", "none")
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60));
        if (idUsuario != null) {
            builder.claim("id_usuario", idUsuario);
        }
        return builder.build();
    }

    private void iniciarSesion(Jwt jwt, String... roles) {
        SecurityContextHolder.getContext().setAuthentication(
                new JwtAuthenticationToken(jwt, AuthorityUtils.createAuthorityList(roles)));
    }

    @Test
    void esVendedorAcotadoSoloSinOtroRolDeAlcanceGeneral() {
        iniciarSesion(token("7"), "ROLE_VENDEDOR");
        assertTrue(Utils.esVendedorAcotado());

        for (String otro : new String[] { "ROLE_ADMIN", "ROLE_COBRADOR", "ROLE_INVENTARIO" }) {
            iniciarSesion(token("7"), "ROLE_VENDEDOR", otro);
            assertFalse(Utils.esVendedorAcotado(), otro);
        }
        iniciarSesion(token("7"), "ROLE_VENDEDOR", "ROLE_BODEGA");
        assertTrue(Utils.esVendedorAcotado());
    }

    @Test
    void sinSesionOSinElRolNoEsVendedor() {
        assertFalse(Utils.esVendedorAcotado());

        iniciarSesion(token("7"), "ROLE_COBRADOR");
        assertFalse(Utils.esVendedorAcotado());
    }

    @Test
    void tomaElIdDelUsuarioDelToken() {
        iniciarSesion(token("42"), "ROLE_VENDEDOR");

        assertEquals(42, Utils.idUsuarioAutenticado());
    }

    @Test
    void sinTokenNoHayIdYUnTokenSinIdSeRechaza() {
        assertNull(Utils.idUsuarioAutenticado());

        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("usuario", "x", "ROLE_VENDEDOR"));
        assertNull(Utils.idUsuarioAutenticado());

        iniciarSesion(token(null), "ROLE_VENDEDOR");
        assertThrows(AccessDeniedException.class, Utils::idUsuarioAutenticado);
    }
}
