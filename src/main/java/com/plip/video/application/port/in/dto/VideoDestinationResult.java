package com.plip.video.application.port.in.dto;

import java.util.UUID;

public record VideoDestinationResult(
		UUID videoUuid,
		String status
) {
}
