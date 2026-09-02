package com.plip.video.adapter.out.storage;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
@ConditionalOnProperty(prefix = "plip.storage", name = "type", havingValue = "local")
@RequiredArgsConstructor
public class LocalObjectStorageService {

	private final LocalFilesystemStorageAdapter storageAdapter;
	private final LocalObjectTokenService tokenService;

	public void putObject(
			String requestUri,
			String contextPath,
			String expires,
			String signature,
			InputStream body
	) throws IOException {
		String objectKey = LocalObjectPathHelper.extractObjectKey(requestUri, contextPath);
		tokenService.verifyOrThrow("PUT", objectKey, expires, signature);

		Path target = storageAdapter.resolvePath(objectKey);
		Files.createDirectories(target.getParent());
		Files.copy(body, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
		if (!Files.isRegularFile(target) || Files.size(target) <= 0) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Uploaded object is empty");
		}
	}

	public LocalObjectReadResult getObject(
			String requestUri,
			String contextPath,
			String expires,
			String signature
	) throws IOException {
		String objectKey = LocalObjectPathHelper.extractObjectKey(requestUri, contextPath);
		tokenService.verifyOrThrow("GET", objectKey, expires, signature);

		Path path = storageAdapter.resolvePath(objectKey);
		if (!Files.isRegularFile(path)) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Object not found");
		}

		return new LocalObjectReadResult(objectKey, new InputStreamResource(Files.newInputStream(path)));
	}

	public static String contentTypeFor(String objectKey) {
		if (objectKey.endsWith(".mp4")) {
			return MediaType.valueOf("video/mp4").toString();
		}
		if (objectKey.endsWith(".jpg") || objectKey.endsWith(".jpeg")) {
			return MediaType.IMAGE_JPEG_VALUE;
		}
		return MediaType.APPLICATION_OCTET_STREAM_VALUE;
	}
}
