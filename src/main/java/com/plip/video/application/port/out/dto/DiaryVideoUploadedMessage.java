package com.plip.video.application.port.out.dto;

import java.time.Instant;
import java.util.UUID;

public record DiaryVideoUploadedMessage(
		UUID themeUuid,
		UUID videoUuid,
		UUID userUuid,
		String caption,
		String thumbnailUrl,
		Instant occurredAt
) {
}
