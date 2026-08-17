package com.plip.video.adapter.out.storage;

import com.plip.video.application.port.out.StoragePort;
import com.plip.video.application.port.out.StoredObject;
import com.plip.video.global.config.AwsProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.InputStream;
import java.util.UUID;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "plip.aws", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class S3StorageAdapter implements StoragePort {

	private final S3Client s3Client;
	private final AwsProperties awsProperties;

	@Override
	public StoredObject uploadRawVideo(UUID videoUuid, InputStream content, long contentLength, String contentType) {
		String relativePath = awsProperties.s3().rawVideoPrefix() + videoUuid + ".mp4";
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
	public String resolvePublicUrl(String relativePath) {
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
