package xyz.pangosoft.dtodo.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

class OAuthTokenControllerTest {

    private static final String CLIENT_ID = "angularapp";
    private static final String CLIENT_SECRET = "pangosoftpuntodeventastore2021";

    private AuthenticationManager authenticationManager;
    private JwtTokenService tokenService;
    private OAuthTokenController controller;

    @BeforeEach
    void setUp() {
        authenticationManager = mock(AuthenticationManager.class);
        tokenService = mock(JwtTokenService.class);
        controller = new OAuthTokenController(
                authenticationManager,
                tokenService);
    }

    @Test
    void passwordGrantKeepsLegacyResponseContract() {
        Authentication authentication = UsernamePasswordAuthenticationToken.authenticated(
                "maria", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        when(authenticationManager.authenticate(any(Authentication.class))).thenReturn(authentication);
        when(tokenService.issueTokens(authentication))
                .thenReturn(new JwtTokenService.TokenPair("access.jwt", "refresh.jwt", 900));

        MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
        parameters.add("grant_type", "password");
        parameters.add("username", "maria");
        parameters.add("password", "correct-password");

        ResponseEntity<Map<String, Object>> response = controller.token(basicCredentials(
                CLIENT_ID, CLIENT_SECRET), parameters);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody())
                .containsEntry("access_token", "access.jwt")
                .containsEntry("refresh_token", "refresh.jwt")
                .containsEntry("token_type", "bearer")
                .containsEntry("expires_in", 900L)
                .containsEntry("scope", "read write");
    }

    @Test
    void rejectsInvalidClientBeforeCheckingUserCredentials() {
        MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
        parameters.add("grant_type", "password");
        parameters.add("username", "maria");
        parameters.add("password", "correct-password");

        ResponseEntity<Map<String, Object>> response = controller.token(basicCredentials(
                CLIENT_ID, "wrong-secret"), parameters);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).containsEntry("error", "invalid_client");
        assertThat(response.getHeaders().getFirst("WWW-Authenticate")).contains("Basic");
        verifyNoInteractions(authenticationManager, tokenService);
    }

    private String basicCredentials(String username, String password) {
        String credentials = username + ":" + password;
        return "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
    }
}
