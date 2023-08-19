package com.aglayatech.licorstore.auth;

import java.util.Arrays;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
@EnableResourceServer
public class ResourceServerConfig extends ResourceServerConfigurerAdapter {

	@Override
	public void configure(HttpSecurity http) throws Exception {
		
		http.authorizeRequests()
		.antMatchers(HttpMethod.GET, "/api/productos", "/api/productos/page/**", "/api/uploads/img/**", "/images/**").permitAll()
		.antMatchers(HttpMethod.GET, "/api/clientes", "/api/clientes/{id}", "/api/clientes/page/**").permitAll()
		.antMatchers(HttpMethod.GET, "/api/marcas", "/api/marcas/page/**").permitAll()
		.antMatchers(HttpMethod.GET, "/api/tipos-producto", "/api/tipos-producto/page/**").permitAll()
		// .antMatchers(HttpMethod.GET, "/api/usuarios", "/api/usuarios/page/**").permitAll()
		.antMatchers(HttpMethod.GET, "/api/roles", "/api/roles/page/**").permitAll()
		.antMatchers(HttpMethod.GET, "/api/facturas/**").permitAll()
		.antMatchers(HttpMethod.GET, "/api/correlativos", "/api/correlativos/page/**").permitAll()
		.antMatchers(HttpMethod.GET, "/api/movimientos", "/api/movimientos/**").permitAll()
		/*.antMatchers(HttpMethod.POST, "/api/productos/upload").hasAnyRole("COBRADOR", "ADMIN")
		.antMatchers(HttpMethod.POST, "/api/productos").hasRole("ADMIN")
		.antMatchers(HttpMethod.GET, "/api/clientes", "/api/clientes/page/**").permitAll()
		.antMatchers(HttpMethod.GET, "/api/clientes/{id}").hasAnyRole("COBRADOR", "ADMIN")
		.antMatchers(HttpMethod.POST, "/api/clientes").hasAnyRole("COBRADOR","ADMIN")
		.antMatchers(HttpMethod.PUT, "/api/clientes").hasAnyRole("COBRADOR", "ADMIN")
		.antMatchers(HttpMethod.GET, "/api/marcas", "/api/marcas/page/**").permitAll()
		.antMatchers("/api/productos/**").hasRole("ADMIN")
		.antMatchers("/api/clientes/**").hasRole("ADMIN")*/
		.anyRequest().authenticated()
		.and().cors().configurationSource(corsConfigurationSource());
	}
	
	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration config = new CorsConfiguration();
		config.setAllowedOriginPatterns(Arrays.asList("*"));
		config.setAllowedMethods(Arrays.asList("GET","POST","PUT","DELETE","OPTIONS"));
		config.setAllowCredentials(true);
		config.setAllowedHeaders(Arrays.asList("Content-Type","Authorization"));
		
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", config);
		
		return source;
	}
	
	@Bean
	public FilterRegistrationBean<CorsFilter> corsFilter(){
		FilterRegistrationBean<CorsFilter> bean = new FilterRegistrationBean<>(new CorsFilter(corsConfigurationSource()));
		bean.setOrder(Ordered.HIGHEST_PRECEDENCE);
		return bean;
	}
}
