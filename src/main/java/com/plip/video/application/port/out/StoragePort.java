package com.plip.video.application.port.out;

import java.util.UUID;

public interface StoragePort {

	String buildRawS3Key(UUID videoUuid);

	PresignedUploadUrl createPresignedPutUrl(UUID videoUuid, String contentType);

	StoredObject headRawObject(String rawS3Key);

	String createPresignedRawPlaybackUrl(String rawS3Key);

	String resolvePublicUrl(String relativePath);
}
