package com.plip.video.application.port.out;

import java.io.InputStream;
import java.util.UUID;

public interface StoragePort {

	StoredObject uploadRawVideo(UUID videoUuid, InputStream content, long contentLength, String contentType);

	StoredObject uploadThumbnail(UUID videoUuid, InputStream content, long contentLength, String contentType);

	String resolvePublicUrl(String relativePath);
}
