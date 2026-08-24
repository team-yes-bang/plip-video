package com.plip.video.global.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class InternalApiKeyFilter extends OncePerRequestFilter {

	public static final String HEADER_NAME = "X-Internal-Api-Key";

	private final InternalProperties internalProperties;

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		String uri = request.getRequestURI();
		return uri == null || !uri.startsWith("/internal/");
	}

	@Override
	protected void doFilterInternal(
			HttpServletRequest request,
			HttpServletResponse response,
			FilterChain filterChain
	) throws ServletException, IOException {
		String configuredKey = internalProperties.apiKey();
		if (configuredKey == null || configuredKey.isBlank()) {
			response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Internal API key is not configured");
			return;
		}

		String providedKey = request.getHeader(HEADER_NAME);
		if (!constantTimeEquals(configuredKey, providedKey)) {
			response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid internal API key");
			return;
		}

		filterChain.doFilter(request, response);
	}

	private static boolean constantTimeEquals(String expected, String actual) {
		if (expected == null || actual == null) {
			return false;
		}
		return MessageDigest.isEqual(
				expected.getBytes(StandardCharsets.UTF_8),
				actual.getBytes(StandardCharsets.UTF_8)
		);
	}
}
