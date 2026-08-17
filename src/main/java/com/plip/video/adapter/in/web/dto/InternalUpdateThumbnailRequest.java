package com.plip.video.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Lambda → thumbnail_image_path 갱신")
public record InternalUpdateThumbnailRequest(
		@NotBlank
		@Schema(description = "S3 상대 경로", example = "images/thumbnails/{videoUuid}.jpg")
		String thumbnailS3Key
) {
}
