package com.plip.video.adapter.out.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.plip.video.adapter.out.persistence.entity.VideoProcessingOutboxEntity;
import com.plip.video.adapter.out.persistence.entity.VideoProcessingOutboxEventType;
import com.plip.video.adapter.out.persistence.repository.VideoProcessingOutboxJpaRepository;
import com.plip.video.application.port.out.VideoProcessingOutboxPort;
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
public class VideoProcessingOutboxAdapter implements VideoProcessingOutboxPort {

	private final VideoProcessingOutboxJpaRepository videoProcessingOutboxJpaRepository;
	private final ObjectMapper objectMapper;

	@Override
	@Transactional
	public void enqueueProcessingJobs(
			UUID videoUuid,
			String rawS3Key,
			String caption,
			String overlayTime,
			int maxDurationSeconds,
			boolean invokeThumbnailLambda
	) {
		LocalDateTime now = LocalDateTime.now();
		if (invokeThumbnailLambda) {
			saveIfAbsent(videoUuid, VideoProcessingOutboxEventType.THUMBNAIL_INVOKE, thumbnailPayload(rawS3Key), now);
		}
		saveIfAbsent(
				videoUuid,
				VideoProcessingOutboxEventType.SQS_ENQUEUE,
				sqsPayload(rawS3Key, caption, overlayTime, maxDurationSeconds),
				now
		);
	}

	private void saveIfAbsent(
			UUID videoUuid,
			VideoProcessingOutboxEventType eventType,
			String payloadJson,
			LocalDateTime now
	) {
		try {
			videoProcessingOutboxJpaRepository.save(VideoProcessingOutboxEntity.pending(
					videoUuid,
					eventType,
					payloadJson,
					now
			));
		} catch (DataIntegrityViolationException exception) {
			// Idempotent complete retry — outbox row already exists.
		}
	}

	private String thumbnailPayload(String rawS3Key) {
		return writeJson(Map.of("rawS3Key", rawS3Key));
	}

	private String sqsPayload(String rawS3Key, String caption, String overlayTime, int maxDurationSeconds) {
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("rawS3Key", rawS3Key);
		if (caption != null && !caption.isBlank()) {
			payload.put("caption", caption);
		}
		if (overlayTime != null && !overlayTime.isBlank()) {
			payload.put("overlayTime", overlayTime);
		}
		payload.put("maxDurationSeconds", maxDurationSeconds);
		return writeJson(payload);
	}

	private String writeJson(Map<String, Object> payload) {
		try {
			return objectMapper.writeValueAsString(payload);
		} catch (JsonProcessingException exception) {
			throw new IllegalStateException("Failed to serialize outbox payload", exception);
		}
	}

}
