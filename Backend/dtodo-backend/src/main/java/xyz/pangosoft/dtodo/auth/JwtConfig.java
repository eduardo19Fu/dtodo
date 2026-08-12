package xyz.pangosoft.dtodo.auth;

import java.util.Base64;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.proc.SecurityContext;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

@Configuration
public class JwtConfig {

    private static final int MINIMUM_SECRET_BYTES = 32;
    private static final String JWT_SECRET_BASE64 = "1faC4shiPBjPz/GEhw3p/tsG8A8jZqZ9To9iHx8119s=";

    @Bean
    public SecretKey jwtSecretKey() {
        byte[] secret;
        try {
            secret = Base64.getDecoder().decode(JWT_SECRET_BASE64);
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("JWT_SECRET_BASE64 debe estar codificado en Base64", exception);
        }

        if (secret.length < MINIMUM_SECRET_BYTES) {
            throw new IllegalStateException("JWT_SECRET_BASE64 debe contener al menos 256 bits");
        }

        return new SecretKeySpec(secret, "HmacSHA256");
    }

    @Bean
    public JwtEncoder jwtEncoder(SecretKey secretKey) {
        return new NimbusJwtEncoder(new ImmutableSecret<SecurityContext>(secretKey));
    }

    @Bean
    @Primary
    public JwtDecoder jwtDecoder(
            SecretKey secretKey,
            @Value("${dtodo.security.jwt-issuer:dtodo-api}") String issuer,
            @Value("${dtodo.security.client-id:angularapp}") String clientId) {
        return createDecoder(secretKey, issuer, clientId, "access");
    }

    @Bean("refreshTokenDecoder")
    public JwtDecoder refreshTokenDecoder(
            SecretKey secretKey,
            @Value("${dtodo.security.jwt-issuer:dtodo-api}") String issuer,
            @Value("${dtodo.security.client-id:angularapp}") String clientId) {
        return createDecoder(secretKey, issuer, clientId, "refresh");
    }

    private JwtDecoder createDecoder(SecretKey secretKey, String issuer, String clientId, String tokenType) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(secretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();

        OAuth2TokenValidator<Jwt> issuerValidator = JwtValidators.createDefaultWithIssuer(issuer);
        OAuth2TokenValidator<Jwt> audienceValidator = jwt -> jwt.getAudience().contains(clientId)
                ? OAuth2TokenValidatorResult.success()
                : validationFailure("El audience del token no es válido");
        OAuth2TokenValidator<Jwt> tokenTypeValidator = jwt -> tokenType.equals(jwt.getClaimAsString("token_type"))
                ? OAuth2TokenValidatorResult.success()
                : validationFailure("El tipo de token no es válido");
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                issuerValidator, audienceValidator, tokenTypeValidator));
        return decoder;
    }

    private OAuth2TokenValidatorResult validationFailure(String description) {
        return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", description, null));
    }
}
