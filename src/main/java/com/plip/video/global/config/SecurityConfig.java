package com.plip.video.global.config;

import com.plip.video.global.security.GatewaySignatureVerificationFilter;
import com.plip.video.global.security.JwtAuthenticationEntryPoint;
import com.plip.video.global.security.UserUuidHeaderAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(GatewayHmacProperties.class)
@RequiredArgsConstructor
public class SecurityConfig {

	private final GatewaySignatureVerificationFilter gatewaySignatureVerificationFilter;
	private final UserUuidHeaderAuthenticationFilter userUuidHeaderAuthenticationFilter;
	private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
				.csrf(AbstractHttpConfigurer::disable)
				.formLogin(AbstractHttpConfigurer::disable)
				.httpBasic(AbstractHttpConfigurer::disable)
				.sessionManagement(session ->
						session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.exceptionHandling(exception ->
						exception.authenticationEntryPoint(jwtAuthenticationEntryPoint))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
						.requestMatchers(
								"/api/test",
								"/actuator/health",
								"/actuator/info",
								"/internal/**",
								"/v3/api-docs",
								"/v3/api-docs.yaml",
								"/v3/api-docs/**",
								"/swagger-ui.html",
								"/swagger-ui/**"
						).permitAll()
						.requestMatchers(HttpMethod.GET, "/api/v1/local-objects/**").permitAll()
						.requestMatchers(HttpMethod.PUT, "/api/v1/local-objects/**").permitAll()
						.requestMatchers("/api/**").authenticated()
						.anyRequest().authenticated()
				)
				.addFilterBefore(
						gatewaySignatureVerificationFilter,
						UsernamePasswordAuthenticationFilter.class
				)
				.addFilterBefore(
						userUuidHeaderAuthenticationFilter,
						UsernamePasswordAuthenticationFilter.class
				);

		return http.build();
	}
}
