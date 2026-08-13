package xyz.pangosoft.dtodo.auth;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import xyz.pangosoft.dtodo.model.Usuario;
import xyz.pangosoft.dtodo.service.IUsuarioService;

@Service
public class JwtTokenService {

    private final JwtEncoder jwtEncoder;
    private final JwtDecoder jwtDecoder;
    private final UserDetailsService userDetailsService;
    private final IUsuarioService usuarioService;
    private final String issuer;
    private final String clientId;
    private final Duration accessTokenTtl;
    private final Duration refreshTokenTtl;

    public JwtTokenService(
            JwtEncoder jwtEncoder,
            @Qualifier("refreshTokenDecoder") JwtDecoder jwtDecoder,
            UserDetailsService userDetailsService,
            IUsuarioService usuarioService,
            @Value("${dtodo.security.jwt-issuer:dtodo-api}") String issuer,
            @Value("${dtodo.security.client-id:angularapp}") String clientId,
            @Value("${dtodo.security.access-token-ttl:PT15M}") Duration accessTokenTtl,
            @Value("${dtodo.security.refresh-token-ttl:PT8H}") Duration refreshTokenTtl) {
        this.jwtEncoder = jwtEncoder;
        this.jwtDecoder = jwtDecoder;
        this.userDetailsService = userDetailsService;
        this.usuarioService = usuarioService;
        this.issuer = issuer;
        this.clientId = clientId;
        this.accessTokenTtl = accessTokenTtl;
        this.refreshTokenTtl = refreshTokenTtl;
    }

    public TokenPair issueTokens(Authentication authentication) {
        Usuario usuario = usuarioService.findByUsuario(authentication.getName());
        Instant issuedAt = Instant.now();
        String accessToken = encodeAccessToken(authentication, usuario, issuedAt);
        String refreshToken = encodeRefreshToken(authentication.getName(), issuedAt);
        return new TokenPair(accessToken, refreshToken, accessTokenTtl.toSeconds());
    }

    public TokenPair refresh(String refreshToken) {
        Jwt jwt = jwtDecoder.decode(refreshToken);
        if (!"refresh".equals(jwt.getClaimAsString("token_type")) || !jwt.getAudience().contains(clientId)) {
            throw new IllegalArgumentException("Token de actualización inválido");
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(jwt.getSubject());
        if (!userDetails.isEnabled()) {
            throw new DisabledException("El usuario está deshabilitado");
        }

        Authentication authentication = UsernamePasswordAuthenticationToken.authenticated(
                userDetails, null, userDetails.getAuthorities());
        return issueTokens(authentication);
    }

    private String encodeAccessToken(Authentication authentication, Usuario usuario, Instant issuedAt) {
        List<String> authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .distinct()
                .toList();

        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject(authentication.getName())
                .audience(List.of(clientId))
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plus(accessTokenTtl))
                .id(UUID.randomUUID().toString())
                .claim("token_type", "access")
                .claim("user_name", authentication.getName())
                .claim("authorities", authorities)
                .claim("scope", "read write")
                .claim("id_usuario", usuario.getIdUsuario().toString());

        addClaimIfPresent(claims, "primerNombre", usuario.getPrimerNombre());
        addClaimIfPresent(claims, "segundoNombre", usuario.getSegundoNombre());
        addClaimIfPresent(claims, "apellido", usuario.getApellido());

        return encode(claims.build());
    }

    private String encodeRefreshToken(String username, Instant issuedAt) {
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject(username)
                .audience(List.of(clientId))
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plus(refreshTokenTtl))
                .id(UUID.randomUUID().toString())
                .claim("token_type", "refresh")
                .build();
        return encode(claims);
    }

    private String encode(JwtClaimsSet claims) {
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).type("JWT").build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    private void addClaimIfPresent(JwtClaimsSet.Builder claims, String name, String value) {
        if (value != null) {
            claims.claim(name, value);
        }
    }

    public record TokenPair(String accessToken, String refreshToken, long expiresIn) {
    }
}