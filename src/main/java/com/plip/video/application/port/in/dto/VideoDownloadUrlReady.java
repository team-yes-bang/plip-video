package com.plip.video.application.port.in.dto;

import java.util.UUID;

public record VideoDownloadUrlReady(
		UUID videoUuid,
		String downloadUrl
) implements VideoDownloadUrlResult {
}
