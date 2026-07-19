package xyz.pangosoft.dtodo.auth;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Configuración independiente para el codificador de contraseñas.
 *
 * <p>Se extrae de {@link SpringSecurityConfig} para evitar una dependencia
 * circular: {@code SpringSecurityConfig} inyecta el {@code UserDetailsService}
 * (implementado por {@code UsuarioServiceImpl}) y, a su vez, la capa de servicio
 * de usuarios necesita el {@link BCryptPasswordEncoder} para cifrar contraseñas.
 * Al vivir el bean en una configuración sin dependencias, ambos consumidores lo
 * resuelven sin ciclos.</p>
 */
@Configuration
public class EncoderConfig {

	@Bean
	public BCryptPasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

}
