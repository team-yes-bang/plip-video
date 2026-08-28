package com.plip.video.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "썸네일 Presigned PUT URL 발급 응답")
public record VideoThumbnailUploadUrlResponse(
		@Schema(description = "영상 UUID") UUID videoUuid,
		@Schema(description = "S3 object key (processed bucket)", example = "thumbnail/{videoUuid}.jpg")
		String thumbnailS3Key,
		@Schema(description = "Presigned PUT URL") String uploadUrl,
		@Schema(description = "Presigned URL 만료 시각 (UTC)") Instant expiresAt
) {
}
