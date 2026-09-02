package com.plip.video.adapter.out.storage;

import com.plip.video.application.port.out.PresignedUploadUrl;
import com.plip.video.application.port.out.StoragePort;
import com.plip.video.application.port.out.StoredObject;
import com.plip.video.global.config.AwsProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "plip.storage", name = "type", havingValue = "noop", matchIfMissing = true)
@RequiredArgsConstructor
public class NoOpStorageAdapter implements StoragePort {

	private static final long STUB_OBJECT_SIZE_BYTES = 1024L;

	private final AwsProperties awsProperties;

	@Override
	public String buildRawS3Key(UUID videoUuid) {
		return awsProperties.s3().rawVideoPrefix() + videoUuid + ".mp4";
	}

	@Override
	public PresignedUploadUrl createPresignedPutUrl(UUID videoUuid, String contentType, long contentLengthBytes) {
		String rawS3Key = buildRawS3Key(videoUuid);
		Instant expiresAt = Instant.now().plus(Duration.ofSeconds(awsProperties.presignedUrlTtlSeconds()));
		String uploadUrl = "http://localhost/stub-presigned-put/" + rawS3Key
				+ "?contentLength=" + contentLengthBytes;
		log.warn("AWS disabled — stub presigned PUT URL for {}", rawS3Key);
		return new PresignedUploadUrl(rawS3Key, uploadUrl, expiresAt);
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
		String thumbnailS3Key = buildThumbnailS3Key(videoUuid);
		Instant expiresAt = Instant.now().plus(Duration.ofSeconds(awsProperties.presignedUrlTtlSeconds()));
		String uploadUrl = "http://localhost/stub-presigned-put/" + thumbnailS3Key
				+ "?contentLength=" + contentLengthBytes;
		log.warn("AWS disabled — stub presigned PUT URL for {}", thumbnailS3Key);
		return new PresignedUploadUrl(thumbnailS3Key, uploadUrl, expiresAt);
	}

	@Override
	public StoredObject headRawObject(String rawS3Key) {
		log.warn("AWS disabled — stub head object for {}", rawS3Key);
		return new StoredObject(rawS3Key, STUB_OBJECT_SIZE_BYTES);
	}

	@Override
	public StoredObject headThumbnailObject(String thumbnailS3Key) {
		log.warn("AWS disabled — stub head object for {}", thumbnailS3Key);
		return new StoredObject(thumbnailS3Key, STUB_OBJECT_SIZE_BYTES);
	}

	@Override
	public String createPresignedRawPlaybackUrl(String rawS3Key) {
		String playbackUrl = "http://localhost/stub-presigned-get/" + rawS3Key;
		log.warn("AWS disabled — stub presigned GET URL for {}", rawS3Key);
		return playbackUrl;
	}

	@Override
	public String resolvePublicUrl(String relativePath) {
		if (relativePath == null || relativePath.isBlank()) {
			return null;
		}
		return "/stub-media/" + relativePath;
	}
}
