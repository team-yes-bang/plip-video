package com.plip.video.adapter.in.web.controller;

import com.plip.video.adapter.out.storage.LocalFilesystemStorageAdapter;
import com.plip.video.adapter.out.storage.LocalObjectPathHelper;
import com.plip.video.adapter.out.storage.LocalObjectTokenService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

@RestController
@ConditionalOnProperty(prefix = "plip.storage", name = "type", havingValue = "local")
@RequiredArgsConstructor
public class LocalObjectController {

	private final LocalFilesystemStorageAdapter storageAdapter;
	private final LocalObjectTokenService tokenService;

	@PutMapping("/api/v1/local-objects/**")
	public ResponseEntity<Void> putObject(
			HttpServletRequest request,
			@RequestParam("expires") String expires,
			@RequestParam("sig") String signature
	) throws IOException {
		String objectKey = LocalObjectPathHelper.extractObjectKey(
				request.getRequestURI(),
				request.getContextPath()
		);
		tokenService.verifyOrThrow("PUT", objectKey, expires, signature);

		Path target = storageAdapter.resolvePath(objectKey);
		Files.createDirectories(target.getParent());
		try (InputStream body = request.getInputStream()) {
			Files.copy(body, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
		}
		if (!Files.isRegularFile(target) || Files.size(target) <= 0) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Uploaded object is empty");
		}
		return ResponseEntity.ok().build();
	}

	@GetMapping("/api/v1/local-objects/**")
	public ResponseEntity<InputStreamResource> getObject(
			HttpServletRequest request,
			@RequestParam("expires") String expires,
			@RequestParam("sig") String signature
	) throws IOException {
		String objectKey = LocalObjectPathHelper.extractObjectKey(
				request.getRequestURI(),
				request.getContextPath()
		);
		tokenService.verifyOrThrow("GET", objectKey, expires, signature);

		Path path = storageAdapter.resolvePath(objectKey);
		if (!Files.isRegularFile(path)) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Object not found");
		}

		InputStreamResource resource = new InputStreamResource(Files.newInputStream(path));
		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_TYPE, contentTypeFor(objectKey))
				.body(resource);
	}

	private static String contentTypeFor(String objectKey) {
		if (objectKey.endsWith(".mp4")) {
			return MediaType.valueOf("video/mp4").toString();
		}
		if (objectKey.endsWith(".jpg") || objectKey.endsWith(".jpeg")) {
			return MediaType.IMAGE_JPEG_VALUE;
		}
		return MediaType.APPLICATION_OCTET_STREAM_VALUE;
	}
}
