package com.plip.video.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

@Schema(description = "Lambda → processed_path 갱신")
public record InternalUpdateProcessedRequest(
		@NotBlank
		@Schema(description = "S3 상대 경로", example = "videos/processed/{videoUuid}.mp4")
		String processedS3Key,

		@NotNull
		@PositiveOrZero
		@Schema(description = "원본(또는 가공본) 재생 길이(초). maxDurationSeconds 초과 시 거부", example = "4")
		Integer durationSeconds
) {
}
