package xyz.pangosoft.dtodo.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.List;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidationException;

import xyz.pangosoft.dtodo.model.Usuario;
import xyz.pangosoft.dtodo.service.IUsuarioService;

class JwtTokenServiceTest {

    private static final String ISSUER = "dtodo-api";
    private static final String CLIENT_ID = "angularapp";

    private JwtDecoder jwtDecoder;
    private JwtDecoder refreshTokenDecoder;
    private UserDetailsService userDetailsService;
    private IUsuarioService usuarioService;
    private JwtTokenService tokenService;
    private Usuario usuario;

    @BeforeEach
    void setUp() {
        JwtConfig jwtConfig = new JwtConfig();
        SecretKey secretKey = jwtConfig.jwtSecretKey();
        JwtEncoder jwtEncoder = jwtConfig.jwtEncoder(secretKey);
        jwtDecoder = jwtConfig.jwtDecoder(secretKey, ISSUER, CLIENT_ID);
        refreshTokenDecoder = jwtConfig.refreshTokenDecoder(secretKey, ISSUER, CLIENT_ID);

        userDetailsService = mock(UserDetailsService.class);
        usuarioService = mock(IUsuarioService.class);
        tokenService = new JwtTokenService(
                jwtEncoder,
                refreshTokenDecoder,
                userDetailsService,
                usuarioService,
                ISSUER,
                CLIENT_ID,
                Duration.ofMinutes(15),
                Duration.ofHours(8));

        usuario = Usuario.builder()
                .idUsuario(7)
                .usuario("maria")
                .primerNombre("María")
                .apellido("Pérez")
                .enabled(true)
                .build();
        when(usuarioService.findByUsuario("maria")).thenReturn(usuario);
    }

    @Test
    void accessTokenPreservesAllUserRolesAndExpectedClaims() {
        Authentication authentication = UsernamePasswordAuthenticationToken.authenticated(
                "maria",
                null,
                List.of(
                        new SimpleGrantedAuthority("ROLE_ADMIN"),
                        new SimpleGrantedAuthority("ROLE_INVENTARIO")));

        JwtTokenService.TokenPair tokens = tokenService.issueTokens(authentication);
        Jwt accessToken = jwtDecoder.decode(tokens.accessToken());

        assertThat(accessToken.getSubject()).isEqualTo("maria");
        assertThat(accessToken.getClaimAsString("user_name")).isEqualTo("maria");
        assertThat(accessToken.getClaimAsStringList("authorities"))
                .containsExactly("ROLE_ADMIN", "ROLE_INVENTARIO");
        assertThat(accessToken.getClaimAsString("id_usuario")).isEqualTo("7");
        assertThat(accessToken.getAudience()).containsExactly(CLIENT_ID);
        assertThat(accessToken.getExpiresAt()).isAfter(accessToken.getIssuedAt());
        assertThat(tokens.expiresIn()).isEqualTo(Duration.ofMinutes(15).toSeconds());
    }

    @Test
    void accessDecoderRejectsRefreshTokens() {
        Authentication authentication = UsernamePasswordAuthenticationToken.authenticated(
                "maria", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

        JwtTokenService.TokenPair tokens = tokenService.issueTokens(authentication);

        assertThatThrownBy(() -> jwtDecoder.decode(tokens.refreshToken()))
                .isInstanceOf(JwtValidationException.class);
    }

    @Test
    void refreshTokenReloadsCurrentAuthorities() {
        UserDetails currentUser = User.withUsername("maria")
                .password("unused")
                .authorities("ROLE_COBRADOR", "ROLE_INVENTARIO")
                .build();
        when(userDetailsService.loadUserByUsername("maria")).thenReturn(currentUser);

        Authentication originalAuthentication = UsernamePasswordAuthenticationToken.authenticated(
                "maria", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        JwtTokenService.TokenPair originalTokens = tokenService.issueTokens(originalAuthentication);

        JwtTokenService.TokenPair refreshedTokens = tokenService.refresh(originalTokens.refreshToken());
        Jwt refreshedAccessToken = jwtDecoder.decode(refreshedTokens.accessToken());

        assertThat(refreshedAccessToken.getClaimAsStringList("authorities"))
                .containsExactly("ROLE_COBRADOR", "ROLE_INVENTARIO");
    }
}
