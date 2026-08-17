package com.plip.video.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "다운로드 URL 요청 — 가공 진행 중 (202)")
public record VideoDownloadUrlProcessingResponse(
		@Schema(description = "상태", example = "PROCESSING") String status,
		@Schema(description = "영상 UUID") UUID videoUuid,
		@Schema(description = "재시도 권장 간격(초)", example = "3") int retryAfterSeconds,
		@Schema(description = "안내 메시지") String message
) {
}
