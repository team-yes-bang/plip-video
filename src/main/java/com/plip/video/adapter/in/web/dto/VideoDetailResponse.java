package com.plip.video.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "영상 상세 조회 응답 (피드 재생용)")
public record VideoDetailResponse(
		@Schema(description = "영상 UUID") UUID videoUuid,
		@Schema(description = "업로더 UUID") UUID userUuid,
		@Schema(description = "캡션 (없으면 null)") String caption,
		@Schema(description = "업로드 시각 (created_at)") LocalDateTime createdAt,
		@Schema(description = "원본(raw) 영상 Presigned GET URL (TTL 3h)") String rawPlaybackUrl,
		@Schema(description = "썸네일 URL (Lambda callback 전 null)") String thumbnailUrl,
		@Schema(description = "HH:mm 오버레이용 시각 (KST, created_at 기준)") String overlayTime,
		@Schema(description = "다운로드용 가공본 준비 여부 (processed_path 존재)") boolean downloadReady
) {
}
