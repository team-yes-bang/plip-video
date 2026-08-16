package com.plip.video.application.port.in.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record VideoRegisterResult(
		UUID videoUuid,
		String caption,
		LocalDateTime createdAt,
		String thumbnailUrl
) {
}
