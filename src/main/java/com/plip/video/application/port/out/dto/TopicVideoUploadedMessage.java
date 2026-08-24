package com.plip.video.application.port.out.dto;

import java.time.Instant;
import java.util.UUID;

public record TopicVideoUploadedMessage(
		UUID topicUuid,
		UUID videoUuid,
		UUID userUuid,
		String caption,
		Instant occurredAt
) {
}
