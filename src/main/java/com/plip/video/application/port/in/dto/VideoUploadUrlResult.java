package com.plip.video.application.port.in.dto;

import java.time.Instant;
import java.util.UUID;

public record VideoUploadUrlResult(
		UUID videoUuid,
		String rawS3Key,
		String uploadUrl,
		Instant expiresAt
) {
}
