package com.plip.video.adapter.out.storage;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.stream.Collectors;

public final class LocalObjectPathHelper {

	private static final String OBJECT_PREFIX = "/api/v1/local-objects/";

	private LocalObjectPathHelper() {
	}

	public static String encodeObjectKey(String objectKey) {
		return Arrays.stream(objectKey.split("/", -1))
				.map(LocalObjectPathHelper::encodeSegment)
				.collect(Collectors.joining("/"));
	}

	public static String extractObjectKey(String requestUri, String contextPath) {
		String path = requestUri;
		if (contextPath != null && !contextPath.isBlank() && path.startsWith(contextPath)) {
			path = path.substring(contextPath.length());
		}
		if (!path.startsWith(OBJECT_PREFIX)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid local object path");
		}
		String encoded = path.substring(OBJECT_PREFIX.length());
		if (encoded.isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing object key");
		}
		return decodeObjectKey(encoded);
	}

	public static Path resolveStoragePath(Path root, String objectKey) {
		if (objectKey == null || objectKey.isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing object key");
		}
		if (objectKey.contains("..") || objectKey.startsWith("/") || objectKey.contains("\\")) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid object key");
		}
		Path normalizedRoot = root.toAbsolutePath().normalize();
		Path resolved = normalizedRoot.resolve(objectKey).normalize();
		if (!resolved.startsWith(normalizedRoot)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid object key");
		}
		return resolved;
	}

	private static String decodeObjectKey(String encodedKey) {
		return Arrays.stream(encodedKey.split("/", -1))
				.map(LocalObjectPathHelper::decodeSegment)
				.collect(Collectors.joining("/"));
	}

	private static String encodeSegment(String segment) {
		return java.net.URLEncoder.encode(segment, StandardCharsets.UTF_8).replace("+", "%20");
	}

	private static String decodeSegment(String segment) {
		return URLDecoder.decode(segment, StandardCharsets.UTF_8);
	}
}
