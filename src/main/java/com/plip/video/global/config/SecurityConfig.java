package com.plip.video.global.config;

import com.plip.video.global.security.GatewaySignatureVerificationFilter;
import com.plip.video.global.security.JwtAuthenticationEntryPoint;
import com.plip.video.global.security.UserUuidHeaderAuthenticationFilter;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authorization.AuthorizationDecision;
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

	private static final String LOCAL_OBJECTS_PREFIX = "/api/v1/local-objects/";

	private final GatewaySignatureVerificationFilter gatewaySignatureVerificationFilter;
	private final UserUuidHeaderAuthenticationFilter userUuidHeaderAuthenticationFilter;
	private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
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
						.anyRequest().access((authentication, context) -> {
							if (isLocalObjectRequest(context.getRequest())) {
								return new AuthorizationDecision(true);
							}
							var currentAuth = authentication.get();
							return new AuthorizationDecision(currentAuth != null && currentAuth.isAuthenticated());
						})
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

	private static boolean isLocalObjectRequest(HttpServletRequest request) {
		String uri = request.getRequestURI();
		if (uri == null || !uri.startsWith(LOCAL_OBJECTS_PREFIX)) {
			return false;
		}
		String method = request.getMethod();
		return HttpMethod.GET.matches(method) || HttpMethod.PUT.matches(method);
	}
}
