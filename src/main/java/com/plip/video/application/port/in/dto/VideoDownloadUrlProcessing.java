package com.plip.video.application.port.in.dto;

import java.util.UUID;

public record VideoDownloadUrlProcessing(
		UUID videoUuid,
		int retryAfterSeconds,
		String message
) implements VideoDownloadUrlResult {
}
