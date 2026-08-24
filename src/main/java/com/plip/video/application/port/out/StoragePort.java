package com.plip.video.application.port.out;

import java.util.UUID;

public interface StoragePort {

	String buildRawS3Key(UUID videoUuid);

	/**
	 * Presigned PUT. {@code contentLengthBytes} is signed so the client must upload
	 * exactly that many bytes (cannot exceed the size validated at issue time).
	 */
	PresignedUploadUrl createPresignedPutUrl(UUID videoUuid, String contentType, long contentLengthBytes);

	StoredObject headRawObject(String rawS3Key);

	String createPresignedRawPlaybackUrl(String rawS3Key);

	String resolvePublicUrl(String relativePath);
}
