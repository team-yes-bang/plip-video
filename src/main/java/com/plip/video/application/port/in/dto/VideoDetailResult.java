package com.plip.video.application.port.in.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record VideoDetailResult(
		UUID videoUuid,
		UUID userUuid,
		String caption,
		LocalDateTime createdAt,
		String rawPlaybackUrl,
		String thumbnailUrl,
		String overlayTime,
		boolean downloadReady
) {
}
