package com.plip.video.adapter.out.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.plip.video.adapter.out.persistence.entity.VideoDestinationOutboxEntity;
import com.plip.video.adapter.out.persistence.entity.VideoDestinationOutboxEventType;
import com.plip.video.adapter.out.persistence.repository.VideoDestinationOutboxJpaRepository;
import com.plip.video.application.port.out.VideoDestinationOutboxPort;
import com.plip.video.application.port.out.dto.DiaryVideoUploadedMessage;
import com.plip.video.application.port.out.dto.TopicVideoUploadedMessage;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class VideoDestinationOutboxAdapter implements VideoDestinationOutboxPort {

	private final VideoDestinationOutboxJpaRepository videoDestinationOutboxJpaRepository;
	private final ObjectMapper objectMapper;

	@Override
	@Transactional
	public void enqueueTopicVideoUploaded(TopicVideoUploadedMessage message) {
		LocalDateTime now = LocalDateTime.now();
		saveIfAbsent(
				message.videoUuid(),
				VideoDestinationOutboxEventType.TOPIC_VIDEO_UPLOADED,
				message.topicUuid(),
				topicPayload(message),
				now
		);
	}

	@Override
	@Transactional
	public void enqueueDiaryVideoUploaded(DiaryVideoUploadedMessage message) {
		LocalDateTime now = LocalDateTime.now();
		saveIfAbsent(
				message.videoUuid(),
				VideoDestinationOutboxEventType.DIARY_VIDEO_UPLOADED,
				message.themeUuid(),
				diaryPayload(message),
				now
		);
	}

	private void saveIfAbsent(
			UUID videoUuid,
			VideoDestinationOutboxEventType eventType,
			UUID destinationUuid,
			String payloadJson,
			LocalDateTime now
	) {
		try {
			videoDestinationOutboxJpaRepository.save(VideoDestinationOutboxEntity.pending(
					videoUuid,
					eventType,
					destinationUuid,
					payloadJson,
					now
			));
		} catch (DataIntegrityViolationException exception) {
			// destination API idempotent retry
		}
	}

	private String topicPayload(TopicVideoUploadedMessage message) {
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("topicUuid", message.topicUuid().toString());
		payload.put("videoUuid", message.videoUuid().toString());
		payload.put("userUuid", message.userUuid().toString());
		payload.put("caption", message.caption());
		payload.put("occurredAt", message.occurredAt().toString());
		return writeJson(payload);
	}

	private String diaryPayload(DiaryVideoUploadedMessage message) {
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("themeUuid", message.themeUuid().toString());
		payload.put("videoUuid", message.videoUuid().toString());
		payload.put("userUuid", message.userUuid().toString());
		payload.put("caption", message.caption());
		if (message.thumbnailUrl() != null) {
			payload.put("thumbnailUrl", message.thumbnailUrl());
		}
		payload.put("occurredAt", message.occurredAt().toString());
		return writeJson(payload);
	}

	private String writeJson(Map<String, Object> payload) {
		try {
			return objectMapper.writeValueAsString(payload);
		} catch (JsonProcessingException exception) {
			throw new IllegalStateException("Failed to serialize destination outbox payload", exception);
		}
	}

}
