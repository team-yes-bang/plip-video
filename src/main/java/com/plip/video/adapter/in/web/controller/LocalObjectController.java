package com.plip.video.adapter.in.web.controller;

import com.plip.video.adapter.out.storage.LocalObjectReadResult;
import com.plip.video.adapter.out.storage.LocalObjectStorageService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@ConditionalOnProperty(prefix = "plip.storage", name = "type", havingValue = "local")
@RequiredArgsConstructor
public class LocalObjectController {

	private final LocalObjectStorageService localObjectStorageService;

	@PutMapping("/api/v1/local-objects/**")
	public ResponseEntity<Void> putObject(
			HttpServletRequest request,
			@RequestParam("expires") String expires,
			@RequestParam("sig") String signature
	) throws IOException {
		localObjectStorageService.putObject(
				request.getRequestURI(),
				request.getContextPath(),
				expires,
				signature,
				request.getInputStream()
		);
		return ResponseEntity.ok().build();
	}

	@GetMapping("/api/v1/local-objects/**")
	public ResponseEntity<InputStreamResource> getObject(
			HttpServletRequest request,
			@RequestParam("expires") String expires,
			@RequestParam("sig") String signature
	) throws IOException {
		LocalObjectReadResult result = localObjectStorageService.getObject(
				request.getRequestURI(),
				request.getContextPath(),
				expires,
				signature
		);
		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_TYPE, LocalObjectStorageService.contentTypeFor(result.objectKey()))
				.body(result.resource());
	}
}
