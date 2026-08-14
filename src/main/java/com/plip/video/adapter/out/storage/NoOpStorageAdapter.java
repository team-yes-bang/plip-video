package com.plip.video.adapter.out.storage;

import com.plip.video.application.port.out.StoragePort;
import com.plip.video.application.port.out.StoredObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.UUID;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "plip.aws", name = "enabled", havingValue = "false", matchIfMissing = true)
public class NoOpStorageAdapter implements StoragePort {

	@Override
	public StoredObject uploadRawVideo(UUID videoUuid, InputStream content, long contentLength, String contentType) {
		String relativePath = "videos/raw/" + videoUuid + ".mp4";
		log.warn("AWS disabled — stub upload for raw video: {}", relativePath);
		return new StoredObject(relativePath, contentLength);
	}

	@Override
	public StoredObject uploadThumbnail(UUID videoUuid, InputStream content, long contentLength, String contentType) {
		String relativePath = "images/thumbnails/" + videoUuid + ".jpg";
		log.warn("AWS disabled — stub upload for thumbnail: {}", relativePath);
		return new StoredObject(relativePath, contentLength);
	}

	@Override
	public String resolvePublicUrl(String relativePath) {
		return "/stub-media/" + relativePath;
	}
}
