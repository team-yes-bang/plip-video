package com.plip.video.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Presigned URL 발급 응답")
public record VideoUploadUrlResponse(
		@Schema(description = "영상 UUID (UUIDv7)") UUID videoUuid,
		@Schema(description = "S3 object key (raw bucket)") String rawS3Key,
		@Schema(description = "Presigned PUT URL") String uploadUrl,
		@Schema(description = "Presigned URL 만료 시각 (UTC)") Instant expiresAt
) {
}
