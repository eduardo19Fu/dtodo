package xyz.pangosoft.dtodo.util;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UtilsObtenerIdUsuarioTest {

    private Jwt jwtConClaims(String claim, Object valor) {
        Jwt.Builder builder = Jwt.withTokenValue("token").header("alg", "none")
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60)).subject("usuario");
        if (claim != null) {
            builder.claim(claim, valor);
        }
        return builder.build();
    }

    @Test
    void devuelveElIdDelUsuarioDelClaimIdUsuario() {
        assertEquals(42, Utils.obtenerIdUsuario(jwtConClaims("id_usuario", "42")));
    }

    @Test
    void rechazaUnTokenAusenteOSinElClaim() {
        assertThrows(AccessDeniedException.class, () -> Utils.obtenerIdUsuario(null));
        assertThrows(AccessDeniedException.class, () -> Utils.obtenerIdUsuario(jwtConClaims(null, null)));
        assertThrows(AccessDeniedException.class, () -> Utils.obtenerIdUsuario(jwtConClaims("id_usuario", " ")));
    }

    @Test
    void rechazaUnClaimQueNoEsNumerico() {
        assertThrows(AccessDeniedException.class, () -> Utils.obtenerIdUsuario(jwtConClaims("id_usuario", "abc")));
    }
}
