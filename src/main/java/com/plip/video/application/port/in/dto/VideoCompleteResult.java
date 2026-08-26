package com.plip.video.application.port.in.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record VideoCompleteResult(
		UUID videoUuid,
		String caption,
		LocalDateTime createdAt,
		String overlayTime,
		boolean newlyCreated
) {
}
