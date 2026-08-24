package com.plip.video.application.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.plip.video.adapter.out.persistence.entity.VideoDestinationOutboxEntity;
import com.plip.video.adapter.out.persistence.entity.VideoDestinationOutboxEventType;
import com.plip.video.adapter.out.persistence.repository.VideoDestinationOutboxJpaRepository;
import com.plip.video.application.port.out.VideoDestinationEventPort;
import com.plip.video.application.port.out.dto.DiaryVideoUploadedMessage;
import com.plip.video.application.port.out.dto.TopicVideoUploadedMessage;
import com.plip.video.global.config.VideoProcessingOutboxProperties;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class VideoDestinationOutboxRelay {

	private final VideoDestinationOutboxJpaRepository videoDestinationOutboxJpaRepository;
	private final VideoDestinationEventPort videoDestinationEventPort;
	private final VideoProcessingOutboxProperties videoProcessingOutboxProperties;
	private final ObjectMapper objectMapper;

	@Scheduled(fixedDelayString = "${plip.video.outbox.relay-interval-ms:5000}")
	@Transactional
	public void relayPendingEvents() {
		LocalDateTime now = LocalDateTime.now();
		List<VideoDestinationOutboxEntity> pending = videoDestinationOutboxJpaRepository.findPendingForUpdate(
				now,
				videoProcessingOutboxProperties.relayBatchSizeOrDefault()
		);
		for (VideoDestinationOutboxEntity event : pending) {
			dispatch(event, now);
		}
	}

	private void dispatch(VideoDestinationOutboxEntity event, LocalDateTime now) {
		try {
			Map<String, Object> payload = objectMapper.readValue(
					event.getPayloadJson(),
					new TypeReference<>() {
					}
			);
			if (event.getEventType() == VideoDestinationOutboxEventType.TOPIC_VIDEO_UPLOADED) {
				videoDestinationEventPort.publishTopicVideoUploaded(new TopicVideoUploadedMessage(
						UUID.fromString(required(payload, "topicUuid")),
						UUID.fromString(required(payload, "videoUuid")),
						UUID.fromString(required(payload, "userUuid")),
						optionalString(payload, "caption"),
						Instant.parse(required(payload, "occurredAt"))
				));
			} else {
				videoDestinationEventPort.publishDiaryVideoUploaded(new DiaryVideoUploadedMessage(
						UUID.fromString(required(payload, "themeUuid")),
						UUID.fromString(required(payload, "videoUuid")),
						UUID.fromString(required(payload, "userUuid")),
						optionalString(payload, "caption"),
						optionalString(payload, "thumbnailUrl"),
						Instant.parse(required(payload, "occurredAt"))
				));
			}
			event.markSent(now);
		} catch (Exception exception) {
			handleFailure(event, now, exception);
		}
	}

	private void handleFailure(VideoDestinationOutboxEntity event, LocalDateTime now, Exception exception) {
		if (event.getAttemptCount() + 1 >= videoProcessingOutboxProperties.maxAttemptsOrDefault()) {
			event.markFailed(exception.getMessage());
			log.error(
					"Video destination outbox permanently failed id={} videoUuid={} eventType={}",
					event.getId(),
					event.getVideoUuid(),
					event.getEventType(),
					exception
			);
			return;
		}
		long backoffMs = videoProcessingOutboxProperties.retryBackoffMsOrDefault()
				* (event.getAttemptCount() + 1L);
		event.scheduleRetry(now.plus(Duration.ofMillis(backoffMs)), exception.getMessage());
		log.warn(
				"Video destination outbox retry scheduled id={} videoUuid={} eventType={} attempt={}",
				event.getId(),
				event.getVideoUuid(),
				event.getEventType(),
				event.getAttemptCount(),
				exception
		);
	}

	private static String required(Map<String, Object> payload, String key) {
		Object value = payload.get(key);
		if (value == null) {
			throw new IllegalStateException("Missing destination outbox payload field: " + key);
		}
		return value.toString();
	}

	private static String optionalString(Map<String, Object> payload, String key) {
		Object value = payload.get(key);
		return value == null ? null : value.toString();
	}

}
