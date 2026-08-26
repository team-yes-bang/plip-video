package com.plip.video.global.security;

import com.plip.video.global.web.RequestHeaders;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class UserUuidHeaderAuthenticationFilter extends OncePerRequestFilter {

	@Override
	protected void doFilterInternal(
			HttpServletRequest request,
			HttpServletResponse response,
			FilterChain filterChain
	) throws ServletException, IOException {
		resolveUserUuidHeader(request).ifPresent(this::authenticate);
		filterChain.doFilter(request, response);
	}

	private Optional<String> resolveUserUuidHeader(HttpServletRequest request) {
		String gatewayHeader = request.getHeader(RequestHeaders.USER_UUID_HEADER);
		if (gatewayHeader != null && !gatewayHeader.isBlank()) {
			return Optional.of(gatewayHeader.trim());
		}
		String legacyHeader = request.getHeader(RequestHeaders.USER_UUID_LEGACY);
		if (legacyHeader != null && !legacyHeader.isBlank()) {
			return Optional.of(legacyHeader.trim());
		}
		return Optional.empty();
	}

	private void authenticate(String userUuidHeader) {
		parseUuid(userUuidHeader).ifPresent(userUuid -> {
			UsernamePasswordAuthenticationToken authentication =
					new UsernamePasswordAuthenticationToken(userUuid.toString(), null, List.of());
			SecurityContextHolder.getContext().setAuthentication(authentication);
		});
	}

	private Optional<UUID> parseUuid(String value) {
		try {
			return Optional.of(UUID.fromString(value));
		} catch (IllegalArgumentException exception) {
			return Optional.empty();
		}
	}
}
