package com.plip.video.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "영상 등록 응답")
public record VideoRegisterResponse(
		@Schema(description = "영상 UUID — Agit/Diary 등 타 서비스에서 참조") UUID videoUuid,
		@Schema(description = "캡션 (없으면 null)") String caption,
		@Schema(description = "업로드 시각 (created_at, HH:mm 오버레이용)") LocalDateTime createdAt,
		@Schema(description = "썸네일 URL") String thumbnailUrl
) {
}
