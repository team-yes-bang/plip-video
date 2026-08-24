package com.plip.video.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "영상 destination publish 응답")
public record VideoDestinationResponse(
		@Schema(description = "영상 UUID")
		UUID videoUuid,

		@Schema(description = "Kafka produce 요청 접수 상태", example = "PUBLISHED")
		String status
) {
}
