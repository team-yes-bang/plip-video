package com.plip.video.application.port.out;

import java.time.Instant;

public record PresignedUploadUrl(
		String rawS3Key,
		String uploadUrl,
		Instant expiresAt
) {
}
