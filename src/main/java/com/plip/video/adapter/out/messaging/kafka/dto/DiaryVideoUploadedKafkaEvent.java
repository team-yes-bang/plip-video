package com.plip.video.adapter.out.messaging.kafka.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record DiaryVideoUploadedKafkaEvent(
		UUID themeUuid,
		UUID videoUuid,
		UUID userUuid,
		String caption,
		String thumbnailUrl,
		Instant occurredAt
) {
}
