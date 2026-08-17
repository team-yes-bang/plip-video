package com.plip.video.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Lambda → processed_path 갱신")
public record InternalUpdateProcessedRequest(
		@NotBlank
		@Schema(description = "S3 상대 경로", example = "videos/processed/{videoUuid}.mp4")
		String processedS3Key
) {
}
