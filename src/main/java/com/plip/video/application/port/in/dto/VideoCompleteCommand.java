package com.plip.video.application.port.in.dto;

import java.util.UUID;

public record VideoCompleteCommand(
		UUID videoUuid,
		UUID userUuid,
		String caption
) {
}
