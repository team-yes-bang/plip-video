package com.plip.video.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "다운로드 URL (가공 완료)")
public record VideoDownloadUrlResponse(
		@Schema(description = "영상 UUID") UUID videoUuid,
		@Schema(description = "CloudFront 다운로드 URL (processed bucket)") String downloadUrl
) {
}
