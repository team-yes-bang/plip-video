package com.plip.video.adapter.out.storage;

import com.plip.video.application.port.out.PresignedUploadUrl;
import com.plip.video.application.port.out.StoragePort;
import com.plip.video.application.port.out.StoredObject;
import com.plip.video.global.config.AwsProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "plip.aws", name = "enabled", havingValue = "false", matchIfMissing = true)
@RequiredArgsConstructor
public class NoOpStorageAdapter implements StoragePort {

	private static final long STUB_OBJECT_SIZE_BYTES = 1024L;

	private final AwsProperties awsProperties;

	@Override
	public StoredObject uploadRawVideo(UUID videoUuid, InputStream content, long contentLength, String contentType) {
		String relativePath = buildRawS3Key(videoUuid);
		log.warn("AWS disabled — stub upload for raw video: {}", relativePath);
		return new StoredObject(relativePath, contentLength);
	}

	@Override
	public StoredObject uploadThumbnail(UUID videoUuid, InputStream content, long contentLength, String contentType) {
		String relativePath = awsProperties.s3().imagePrefix() + "thumbnails/" + videoUuid + ".jpg";
		log.warn("AWS disabled — stub upload for thumbnail: {}", relativePath);
		return new StoredObject(relativePath, contentLength);
	}

	@Override
	public String buildRawS3Key(UUID videoUuid) {
		return awsProperties.s3().rawVideoPrefix() + videoUuid + ".mp4";
	}

	@Override
	public PresignedUploadUrl createPresignedPutUrl(UUID videoUuid, String contentType) {
		String rawS3Key = buildRawS3Key(videoUuid);
		Instant expiresAt = Instant.now().plus(Duration.ofSeconds(awsProperties.presignedUrlTtlSeconds()));
		String uploadUrl = "http://localhost/stub-presigned-put/" + rawS3Key;
		log.warn("AWS disabled — stub presigned PUT URL for {}", rawS3Key);
		return new PresignedUploadUrl(rawS3Key, uploadUrl, expiresAt);
	}

	@Override
	public StoredObject headRawObject(String rawS3Key) {
		log.warn("AWS disabled — stub head object for {}", rawS3Key);
		return new StoredObject(rawS3Key, STUB_OBJECT_SIZE_BYTES);
	}

	@Override
	public String resolvePublicUrl(String relativePath) {
		if (relativePath == null || relativePath.isBlank()) {
			return null;
		}
		return "/stub-media/" + relativePath;
	}
}
