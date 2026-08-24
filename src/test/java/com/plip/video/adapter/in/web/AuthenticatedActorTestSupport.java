package com.plip.video.adapter.in.web;

import java.util.List;
import java.util.UUID;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

public final class AuthenticatedActorTestSupport {

	private AuthenticatedActorTestSupport() {
	}

	public static RequestPostProcessor authenticated(UUID userUuid) {
		return request -> {
			UsernamePasswordAuthenticationToken authentication =
					new UsernamePasswordAuthenticationToken(userUuid.toString(), null, List.of());
			SecurityContextHolder.getContext().setAuthentication(authentication);
			return request;
		};
	}
}
