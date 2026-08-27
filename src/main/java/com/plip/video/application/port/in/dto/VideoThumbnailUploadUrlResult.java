package com.plip.video.application.port.in.dto;

import java.time.Instant;
import java.util.UUID;

public record VideoThumbnailUploadUrlResult(
		UUID videoUuid,
		String thumbnailS3Key,
		String uploadUrl,
		Instant expiresAt
) {
}
