package com.plip.video.global.security;

import com.plip.video.global.config.GatewayHmacProperties;
import com.plip.video.global.web.RequestHeaders;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
@RequiredArgsConstructor
public class GatewaySignatureVerificationFilter extends OncePerRequestFilter {

	private final GatewayHmacProperties gatewayHmacProperties;

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		if (!gatewayHmacProperties.enabled()) {
			return true;
		}
		if (HttpMethod.OPTIONS.matches(request.getMethod())) {
			return true;
		}
		String uri = request.getRequestURI();
		return uri == null || !uri.startsWith("/api/");
	}

	@Override
	protected void doFilterInternal(
			HttpServletRequest request,
			HttpServletResponse response,
			FilterChain filterChain
	) throws ServletException, IOException {
		String userUuid = resolveUserUuid(request);
		if (userUuid == null) {
			response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing gateway user header");
			return;
		}

		String timestampHeader = request.getHeader(RequestHeaders.GATEWAY_TIMESTAMP);
		String signatureHeader = request.getHeader(RequestHeaders.GATEWAY_SIGNATURE);
		if (timestampHeader == null || timestampHeader.isBlank()
				|| signatureHeader == null || signatureHeader.isBlank()) {
			response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing gateway signature");
			return;
		}

		long timestampMillis;
		try {
			timestampMillis = Long.parseLong(timestampHeader.trim());
		} catch (NumberFormatException exception) {
			response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid gateway timestamp");
			return;
		}

		long skewMillis = gatewayHmacProperties.maxSkewSecondsOrDefault() * 1000L;
		long now = System.currentTimeMillis();
		if (Math.abs(now - timestampMillis) > skewMillis) {
			response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Expired gateway signature");
			return;
		}

		String canonicalPayload = GatewayHmacVerifier.buildCanonicalPayload(
				timestampMillis,
				request.getMethod(),
				request.getRequestURI(),
				userUuid
		);
		String expectedSignature = GatewayHmacVerifier.sign(gatewayHmacProperties.secret(), canonicalPayload);
		if (!GatewayHmacVerifier.constantTimeEquals(expectedSignature, signatureHeader.trim())) {
			response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid gateway signature");
			return;
		}

		filterChain.doFilter(request, response);
	}

	private String resolveUserUuid(HttpServletRequest request) {
		String gatewayHeader = request.getHeader(RequestHeaders.USER_UUID_HEADER);
		if (gatewayHeader != null && !gatewayHeader.isBlank()) {
			return gatewayHeader.trim();
		}
		String legacyHeader = request.getHeader(RequestHeaders.USER_UUID_LEGACY);
		if (legacyHeader != null && !legacyHeader.isBlank()) {
			return legacyHeader.trim();
		}
		return null;
	}

}
