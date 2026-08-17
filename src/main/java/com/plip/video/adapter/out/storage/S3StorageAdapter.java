package com.plip.video.adapter.out.storage;

import com.plip.video.application.port.out.PresignedUploadUrl;
import com.plip.video.application.port.out.StoragePort;
import com.plip.video.application.port.out.StoredObject;
import com.plip.video.global.config.AwsProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "plip.aws", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class S3StorageAdapter implements StoragePort {

	private final S3Client s3Client;
	private final S3Presigner s3Presigner;
	private final AwsProperties awsProperties;

	@Override
	public StoredObject uploadRawVideo(UUID videoUuid, InputStream content, long contentLength, String contentType) {
		String relativePath = buildRawS3Key(videoUuid);
		upload(relativePath, content, contentLength, contentType);
		return new StoredObject(relativePath, contentLength);
	}

	@Override
	public StoredObject uploadThumbnail(UUID videoUuid, InputStream content, long contentLength, String contentType) {
		String relativePath = awsProperties.s3().imagePrefix() + "thumbnails/" + videoUuid + ".jpg";
		upload(relativePath, content, contentLength, contentType);
		return new StoredObject(relativePath, contentLength);
	}

	@Override
	public String buildRawS3Key(UUID videoUuid) {
		return awsProperties.s3().rawVideoPrefix() + videoUuid + ".mp4";
	}

	@Override
	public PresignedUploadUrl createPresignedPutUrl(UUID videoUuid, String contentType) {
		String rawS3Key = buildRawS3Key(videoUuid);
		Duration ttl = Duration.ofSeconds(awsProperties.presignedUrlTtlSeconds());
		Instant expiresAt = Instant.now().plus(ttl);

		PutObjectRequest putObjectRequest = PutObjectRequest.builder()
				.bucket(awsProperties.s3().rawBucket())
				.key(rawS3Key)
				.contentType(contentType)
				.build();

		PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
				.signatureDuration(ttl)
				.putObjectRequest(putObjectRequest)
				.build();

		String uploadUrl = s3Presigner.presignPutObject(presignRequest).url().toString();
		return new PresignedUploadUrl(rawS3Key, uploadUrl, expiresAt);
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

	private void upload(String relativePath, InputStream content, long contentLength, String contentType) {
		PutObjectRequest request = PutObjectRequest.builder()
				.bucket(awsProperties.s3().rawBucket())
				.key(relativePath)
				.contentType(contentType)
				.build();

		s3Client.putObject(request, RequestBody.fromInputStream(content, contentLength));
		log.info("Uploaded object to s3://{}/{}", awsProperties.s3().rawBucket(), relativePath);
	}
}
