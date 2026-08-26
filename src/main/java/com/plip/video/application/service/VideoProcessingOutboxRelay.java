package com.plip.video.application.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.plip.video.adapter.out.persistence.entity.VideoProcessingOutboxEntity;
import com.plip.video.adapter.out.persistence.entity.VideoProcessingOutboxEventType;
import com.plip.video.adapter.out.persistence.repository.VideoProcessingOutboxJpaRepository;
import com.plip.video.application.port.out.ThumbnailLambdaPort;
import com.plip.video.application.port.out.VideoProcessingQueuePort;
import com.plip.video.global.config.VideoProcessingOutboxProperties;
import java.time.Duration;
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
public class VideoProcessingOutboxRelay {

	private final VideoProcessingOutboxJpaRepository videoProcessingOutboxJpaRepository;
	private final ThumbnailLambdaPort thumbnailLambdaPort;
	private final VideoProcessingQueuePort videoProcessingQueuePort;
	private final VideoProcessingOutboxProperties videoProcessingOutboxProperties;
	private final ObjectMapper objectMapper;

	@Scheduled(fixedDelayString = "${plip.video.outbox.relay-interval-ms:5000}")
	@Transactional
	public void relayPendingEvents() {
		LocalDateTime now = LocalDateTime.now();
		List<VideoProcessingOutboxEntity> pending = videoProcessingOutboxJpaRepository.findPendingForUpdate(
				now,
				videoProcessingOutboxProperties.relayBatchSizeOrDefault()
		);
		for (VideoProcessingOutboxEntity event : pending) {
			dispatch(event, now);
		}
	}

	private void dispatch(VideoProcessingOutboxEntity event, LocalDateTime now) {
		try {
			Map<String, Object> payload = objectMapper.readValue(
					event.getPayloadJson(),
					new TypeReference<>() {
					}
			);
			if (event.getEventType() == VideoProcessingOutboxEventType.THUMBNAIL_INVOKE) {
				UUID videoUuid = event.getVideoUuid();
				String rawS3Key = required(payload, "rawS3Key");
				thumbnailLambdaPort.invokeThumbnailGeneration(videoUuid, rawS3Key);
			} else {
				UUID videoUuid = event.getVideoUuid();
				String rawS3Key = required(payload, "rawS3Key");
				String caption = optionalString(payload, "caption");
				String overlayTime = optionalString(payload, "overlayTime");
				int maxDurationSeconds = optionalInt(payload, "maxDurationSeconds", 5);
				videoProcessingQueuePort.enqueueVideoProcessing(
						videoUuid,
						rawS3Key,
						caption,
						overlayTime,
						maxDurationSeconds
				);
			}
			event.markSent(now);
		} catch (Exception exception) {
			handleFailure(event, now, exception);
		}
	}

	private void handleFailure(VideoProcessingOutboxEntity event, LocalDateTime now, Exception exception) {
		if (event.getAttemptCount() + 1 >= videoProcessingOutboxProperties.maxAttemptsOrDefault()) {
			event.markFailed(exception.getMessage());
			log.error(
					"Video processing outbox permanently failed id={} videoUuid={} eventType={}",
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
				"Video processing outbox retry scheduled id={} videoUuid={} eventType={} attempt={}",
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
			throw new IllegalStateException("Missing outbox payload field: " + key);
		}
		return value.toString();
	}

	private static String optionalString(Map<String, Object> payload, String key) {
		Object value = payload.get(key);
		return value == null ? null : value.toString();
	}

	private static int optionalInt(Map<String, Object> payload, String key, int defaultValue) {
		Object value = payload.get(key);
		if (value == null) {
			return defaultValue;
		}
		if (value instanceof Number number) {
			return number.intValue();
		}
		return Integer.parseInt(value.toString());
	}

}
