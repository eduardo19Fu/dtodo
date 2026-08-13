package xyz.pangosoft.dtodo.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/oauth")
public class OAuthTokenController {

    private static final String BASIC_PREFIX = "Basic ";
    private static final String CLIENT_ID = "angularapp";
    private static final String CLIENT_SECRET = "pangosoftpuntodeventastore2021";

    private final AuthenticationManager authenticationManager;
    private final JwtTokenService tokenService;

    public OAuthTokenController(
            AuthenticationManager authenticationManager,
            JwtTokenService tokenService) {
        this.authenticationManager = authenticationManager;
        this.tokenService = tokenService;
    }

    @PostMapping(value = "/token", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<Map<String, Object>> token(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @RequestParam MultiValueMap<String, String> parameters) {
        if (!isClientAuthenticated(authorization)) {
            return oauthError(HttpStatus.UNAUTHORIZED, "invalid_client", "Credenciales de cliente inválidas", true);
        }

        String grantType = parameters.getFirst("grant_type");
        if ("password".equals(grantType)) {
            return passwordGrant(parameters);
        }
        if ("refresh_token".equals(grantType)) {
            return refreshTokenGrant(parameters);
        }

        return oauthError(HttpStatus.BAD_REQUEST, "unsupported_grant_type",
                "El tipo de autorización no está soportado", false);
    }

    private ResponseEntity<Map<String, Object>> passwordGrant(MultiValueMap<String, String> parameters) {
        String username = parameters.getFirst("username");
        String password = parameters.getFirst("password");
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            return oauthError(HttpStatus.BAD_REQUEST, "invalid_request",
                    "Usuario y contraseña son obligatorios", false);
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(username, password));
            return tokenResponse(tokenService.issueTokens(authentication));
        } catch (AuthenticationException exception) {
            return oauthError(HttpStatus.BAD_REQUEST, "invalid_grant",
                    "Usuario o contraseña inválidos", false);
        }
    }

    private ResponseEntity<Map<String, Object>> refreshTokenGrant(MultiValueMap<String, String> parameters) {
        String refreshToken = parameters.getFirst("refresh_token");
        if (refreshToken == null || refreshToken.isBlank()) {
            return oauthError(HttpStatus.BAD_REQUEST, "invalid_request",
                    "El token de actualización es obligatorio", false);
        }

        try {
            return tokenResponse(tokenService.refresh(refreshToken));
        } catch (JwtException | AuthenticationException | IllegalArgumentException exception) {
            return oauthError(HttpStatus.BAD_REQUEST, "invalid_grant",
                    "Token de actualización inválido o vencido", false);
        }
    }

    private boolean isClientAuthenticated(String authorization) {
        if (authorization == null || !authorization.startsWith(BASIC_PREFIX)) {
            return false;
        }

        try {
            byte[] decoded = Base64.getDecoder().decode(authorization.substring(BASIC_PREFIX.length()));
            String[] credentials = new String(decoded, StandardCharsets.UTF_8).split(":", 2);
            if (credentials.length != 2 || !constantTimeEquals(CLIENT_ID, credentials[0])) {
                return false;
            }
            return constantTimeEquals(CLIENT_SECRET, credentials[1]);
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private boolean constantTimeEquals(String expected, String actual) {
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                actual.getBytes(StandardCharsets.UTF_8));
    }

    private ResponseEntity<Map<String, Object>> tokenResponse(JwtTokenService.TokenPair tokens) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("access_token", tokens.accessToken());
        body.put("token_type", "bearer");
        body.put("refresh_token", tokens.refreshToken());
        body.put("expires_in", tokens.expiresIn());
        body.put("scope", "read write");
        return ResponseEntity.ok(body);
    }

    private ResponseEntity<Map<String, Object>> oauthError(
            HttpStatus status, String error, String description, boolean authenticateClient) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", error);
        body.put("error_description", description);

        ResponseEntity.BodyBuilder response = ResponseEntity.status(status);
        if (authenticateClient) {
            response.header(HttpHeaders.WWW_AUTHENTICATE, "Basic realm=\"oauth2/client\"");
        }
        return response.body(body);
    }
}
