package com.plip.video.application.port.in.dto;

import java.util.UUID;

public record VideoDestinationCommand(
		UUID videoUuid,
		UUID userUuid,
		VideoDestinationKind kind,
		UUID topicUuid,
		UUID themeUuid,
		UUID agitUuid,
		String caption
) {
}
