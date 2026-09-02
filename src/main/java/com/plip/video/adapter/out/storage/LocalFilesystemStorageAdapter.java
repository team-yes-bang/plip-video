package com.plip.video.adapter.out.storage;

import com.plip.video.application.port.out.PresignedUploadUrl;
import com.plip.video.application.port.out.StoragePort;
import com.plip.video.application.port.out.StoredObject;
import com.plip.video.global.config.AwsProperties;
import com.plip.video.global.config.StorageProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "plip.storage", name = "type", havingValue = "local")
@RequiredArgsConstructor
public class LocalFilesystemStorageAdapter implements StoragePort {

	private final AwsProperties awsProperties;
	private final StorageProperties storageProperties;
	private final LocalObjectTokenService tokenService;

	private Path rootPath;
	private String publicBaseUrl;

	@PostConstruct
	void init() throws IOException {
		StorageProperties.LocalProperties local = storageProperties.local();
		if (local == null || local.root() == null || local.root().isBlank()) {
			throw new IllegalStateException("plip.storage.local.root is required when plip.storage.type=local");
		}
		if (local.publicBaseUrl() == null || local.publicBaseUrl().isBlank()) {
			throw new IllegalStateException(
					"plip.storage.local.public-base-url is required when plip.storage.type=local");
		}
		rootPath = Path.of(local.root()).toAbsolutePath().normalize();
		Files.createDirectories(rootPath);
		publicBaseUrl = local.publicBaseUrl().replaceAll("/+$", "");
		log.info("Local storage enabled at {}", rootPath);
	}

	@Override
	public String buildRawS3Key(UUID videoUuid) {
		return awsProperties.s3().rawVideoPrefix() + videoUuid + ".mp4";
	}

	@Override
	public PresignedUploadUrl createPresignedPutUrl(UUID videoUuid, String contentType, long contentLengthBytes) {
		return presignedPutUrl(buildRawS3Key(videoUuid));
	}

	@Override
	public String buildThumbnailS3Key(UUID videoUuid) {
		return awsProperties.s3().thumbnailPrefix() + videoUuid + ".jpg";
	}

	@Override
	public PresignedUploadUrl createPresignedThumbnailPutUrl(
			UUID videoUuid,
			String contentType,
			long contentLengthBytes
	) {
		return presignedPutUrl(buildThumbnailS3Key(videoUuid));
	}

	@Override
	public StoredObject headRawObject(String rawS3Key) {
		return headObject(rawS3Key, "Raw video not found in local storage: ");
	}

	@Override
	public StoredObject headThumbnailObject(String thumbnailS3Key) {
		return headObject(thumbnailS3Key, "Thumbnail not found in local storage: ");
	}

	@Override
	public String createPresignedRawPlaybackUrl(String rawS3Key) {
		return signedUrl("GET", rawS3Key);
	}

	@Override
	public String resolvePublicUrl(String relativePath) {
		if (relativePath == null || relativePath.isBlank()) {
			return null;
		}
		return signedUrl("GET", relativePath);
	}

	Path resolvePath(String objectKey) {
		return LocalObjectPathHelper.resolveStoragePath(rootPath, objectKey);
	}

	private PresignedUploadUrl presignedPutUrl(String objectKey) {
		Duration ttl = Duration.ofSeconds(awsProperties.presignedUrlTtlSeconds());
		Instant expiresAt = Instant.now().plus(ttl);
		long expiresEpoch = expiresAt.getEpochSecond();
		String uploadUrl = signedUrl("PUT", objectKey, expiresEpoch);
		return new PresignedUploadUrl(objectKey, uploadUrl, expiresAt);
	}

	private String signedUrl(String method, String objectKey) {
		Duration ttl = Duration.ofSeconds(awsProperties.presignedUrlTtlSeconds());
		long expiresEpoch = Instant.now().plus(ttl).getEpochSecond();
		return signedUrl(method, objectKey, expiresEpoch);
	}

	private String signedUrl(String method, String objectKey, long expiresEpoch) {
		String signature = tokenService.sign(method, objectKey, expiresEpoch);
		String encodedKey = LocalObjectPathHelper.encodeObjectKey(objectKey);
		return publicBaseUrl
				+ "/api/video/api/v1/local-objects/"
				+ encodedKey
				+ "?expires="
				+ expiresEpoch
				+ "&sig="
				+ signature;
	}

	private StoredObject headObject(String objectKey, String notFoundPrefix) {
		Path path = resolvePath(objectKey);
		if (!Files.isRegularFile(path)) {
			throw new IllegalArgumentException(notFoundPrefix + objectKey);
		}
		try {
			long size = Files.size(path);
			if (size <= 0) {
				throw new IllegalArgumentException(notFoundPrefix + objectKey);
			}
			return new StoredObject(objectKey, size);
		} catch (IOException ex) {
			throw new IllegalArgumentException(notFoundPrefix + objectKey, ex);
		}
	}
}
