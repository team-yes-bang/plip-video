package com.plip.video.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "영상 destination publish 요청 (complete 이후)")
public record VideoDestinationRequest(
		@Schema(description = "부착 대상 종류", example = "TOPIC", requiredMode = Schema.RequiredMode.REQUIRED)
		VideoDestinationKind kind,

		@Schema(description = "아지트 토픽 UUID (kind=TOPIC 필수)", example = "0190abcd-1111-7abc-def0-123456789abc")
		UUID topicUuid,

		@Schema(description = "다이어리 테마 UUID (kind=DIARY 필수). numeric themeId 아님", example = "01912345-6789-7abc-def0-123456789abc")
		UUID themeUuid,

		@Schema(description = "아지트 UUID (kind=TOPIC 선택, 프론트 컨텍스트)", example = "018f3f6e-8e2a-7b3c-9d4e-5f6a7b8c9d0e")
		UUID agitUuid,

		@Schema(description = "캡션 (선택, 없으면 complete 시 저장값 사용)")
		String caption
) {
}
