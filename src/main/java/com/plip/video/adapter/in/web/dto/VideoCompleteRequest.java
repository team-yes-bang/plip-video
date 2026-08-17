package com.plip.video.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "영상 업로드 complete 요청")
public record VideoCompleteRequest(
		@Schema(description = "캡션 (선택)") String caption
) {
}
