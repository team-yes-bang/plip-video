package com.plip.video.adapter.out.storage;

import com.plip.video.application.port.out.PresignedUploadUrl;
import com.plip.video.application.port.out.StoragePort;
import com.plip.video.application.port.out.StoredObject;
import com.plip.video.global.config.AwsProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Component
@ConditionalOnProperty(prefix = "plip.aws", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class S3StorageAdapter implements StoragePort {

	private final S3Client s3Client;
	private final S3Presigner s3Presigner;
	private final AwsProperties awsProperties;

	@Override
	public String buildRawS3Key(UUID videoUuid) {
		return awsProperties.s3().rawVideoPrefix() + videoUuid + ".mp4";
	}

	@Override
	public String buildThumbnailS3Key(UUID videoUuid) {
		return awsProperties.s3().thumbnailPrefix() + videoUuid + ".jpg";
	}

	@Override
	public PresignedUploadUrl createPresignedPutUrl(UUID videoUuid, String contentType, long contentLengthBytes) {
		String rawS3Key = buildRawS3Key(videoUuid);
		Duration ttl = Duration.ofSeconds(awsProperties.presignedUrlTtlSeconds());
		Instant expiresAt = Instant.now().plus(ttl);

		PutObjectRequest putObjectRequest = PutObjectRequest.builder()
				.bucket(awsProperties.s3().rawBucket())
				.key(rawS3Key)
				.contentType(contentType)
				.contentLength(contentLengthBytes)
				.build();

		PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
				.signatureDuration(ttl)
				.putObjectRequest(putObjectRequest)
				.build();

		String uploadUrl = s3Presigner.presignPutObject(presignRequest).url().toString();
		return new PresignedUploadUrl(rawS3Key, uploadUrl, expiresAt);
	}

	@Override
	public PresignedUploadUrl createPresignedThumbnailPutUrl(
			UUID videoUuid,
			String contentType,
			long contentLengthBytes
	) {
		String thumbnailS3Key = buildThumbnailS3Key(videoUuid);
		Duration ttl = Duration.ofSeconds(awsProperties.presignedUrlTtlSeconds());
		Instant expiresAt = Instant.now().plus(ttl);

		PutObjectRequest putObjectRequest = PutObjectRequest.builder()
				.bucket(awsProperties.s3().processedBucket())
				.key(thumbnailS3Key)
				.contentType(contentType)
				.contentLength(contentLengthBytes)
				.build();

		PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
				.signatureDuration(ttl)
				.putObjectRequest(putObjectRequest)
				.build();

		String uploadUrl = s3Presigner.presignPutObject(presignRequest).url().toString();
		return new PresignedUploadUrl(thumbnailS3Key, uploadUrl, expiresAt);
	}

	@Override
	public StoredObject headRawObject(String rawS3Key) {
		try {
			var response = s3Client.headObject(HeadObjectRequest.builder()
					.bucket(awsProperties.s3().rawBucket())
					.key(rawS3Key)
					.build());
			return new StoredObject(rawS3Key, response.contentLength());
		} catch (NoSuchKeyException e) {
			throw new IllegalArgumentException("Raw video not found in S3: " + rawS3Key, e);
		}
	}

	@Override
	public StoredObject headProcessedObject(String processedS3Key) {
		try {
			var response = s3Client.headObject(HeadObjectRequest.builder()
					.bucket(awsProperties.s3().processedBucket())
					.key(processedS3Key)
					.build());
			return new StoredObject(processedS3Key, response.contentLength());
		} catch (NoSuchKeyException e) {
			throw new IllegalArgumentException("Processed object not found in S3: " + processedS3Key, e);
		}
	}

	@Override
	public String createPresignedRawPlaybackUrl(String rawS3Key) {
		Duration ttl = Duration.ofSeconds(awsProperties.presignedUrlTtlSeconds());
		GetObjectRequest getObjectRequest = GetObjectRequest.builder()
				.bucket(awsProperties.s3().rawBucket())
				.key(rawS3Key)
				.build();

		GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
				.signatureDuration(ttl)
				.getObjectRequest(getObjectRequest)
				.build();

		return s3Presigner.presignGetObject(presignRequest).url().toString();
	}

	@Override
	public String resolvePublicUrl(String relativePath) {
		if (relativePath == null || relativePath.isBlank()) {
			return null;
		}
		String cdnBaseUrl = awsProperties.s3().cdnBaseUrl();
		if (cdnBaseUrl == null || cdnBaseUrl.isBlank()) {
			return relativePath;
		}
		return cdnBaseUrl.endsWith("/")
				? cdnBaseUrl + relativePath
				: cdnBaseUrl + "/" + relativePath;
	}
}
