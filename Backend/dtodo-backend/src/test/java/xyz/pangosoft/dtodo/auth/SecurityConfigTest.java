package xyz.pangosoft.dtodo.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.mock.web.MockHttpServletRequest;

class SecurityConfigTest {

    @Test
    void allowsDevelopmentFrontendLoginPreflight() {
        SecurityConfig securityConfig = new SecurityConfig();
        CorsConfigurationSource source = securityConfig.corsConfigurationSource(
                List.of("https://dev.dtodojalapa.xyz"));

        MockHttpServletRequest request = new MockHttpServletRequest(HttpMethod.OPTIONS.name(), "/oauth/token");
        CorsConfiguration configuration = source.getCorsConfiguration(request);

        assertThat(configuration).isNotNull();
        assertThat(configuration.checkOrigin("https://dev.dtodojalapa.xyz"))
                .isEqualTo("https://dev.dtodojalapa.xyz");
        assertThat(configuration.checkHttpMethod(HttpMethod.POST)).contains(HttpMethod.POST);
        assertThat(configuration.checkHeaders(List.of(HttpHeaders.CONTENT_TYPE, HttpHeaders.AUTHORIZATION)))
                .containsExactly(HttpHeaders.CONTENT_TYPE, HttpHeaders.AUTHORIZATION);
        assertThat(configuration.getAllowCredentials()).isTrue();
    }

    @Test
    void rejectsUntrustedOrigin() {
        SecurityConfig securityConfig = new SecurityConfig();
        CorsConfigurationSource source = securityConfig.corsConfigurationSource(
                List.of("https://dev.dtodojalapa.xyz"));

        MockHttpServletRequest request = new MockHttpServletRequest(HttpMethod.OPTIONS.name(), "/oauth/token");
        CorsConfiguration configuration = source.getCorsConfiguration(request);

        assertThat(configuration).isNotNull();
        assertThat(configuration.checkOrigin("https://malicioso.example")).isNull();
    }
}
