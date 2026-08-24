package com.plip.video.adapter.in.web;

import com.plip.video.application.exception.UnauthenticatedActorException;
import java.util.UUID;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class AuthenticatedActor {

	private AuthenticatedActor() {
	}

	public static UUID findUserUuid() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null
				|| !authentication.isAuthenticated()
				|| authentication instanceof AnonymousAuthenticationToken) {
			return null;
		}
		try {
			return UUID.fromString(authentication.getName());
		} catch (IllegalArgumentException exception) {
			return null;
		}
	}

	public static UUID requireUserUuid() {
		UUID userUuid = findUserUuid();
		if (userUuid == null) {
			throw new UnauthenticatedActorException();
		}
		return userUuid;
	}
}
