package com.plip.video.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "영상 업로드 complete 요청")
public record VideoCompleteRequest(
		@Schema(description = "캡션 (선택)") String caption,
		@Schema(description = "클라이언트 업로드 썸네일 S3 key (선택, thumbnail-upload-url 발급값)") String thumbnailS3Key
) {
}
