package com.plip.video.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "영상 업로드 complete 응답")
public record VideoCompleteResponse(
		@Schema(description = "영상 UUID") UUID videoUuid,
		@Schema(description = "캡션 (없으면 null)") String caption,
		@Schema(description = "업로드 시각 (created_at)") LocalDateTime createdAt,
		@Schema(description = "HH:mm 오버레이용 시각 (KST, created_at 기준)") String overlayTime
) {
}
