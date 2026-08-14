package com.plip.video.application.port.in.dto;

import com.plip.video.domain.model.enums.VideoProcessingStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record VideoRegisterResult(
		UUID videoUuid,
		String caption,
		LocalDateTime recordedAt,
		String thumbnailUrl,
		VideoProcessingStatus processingStatus
) {
}
