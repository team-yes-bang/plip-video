package com.plip.video.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
		name = "video_destination_outbox",
		uniqueConstraints = @UniqueConstraint(
				name = "uk_destination_outbox",
				columnNames = {"video_uuid", "event_type", "destination_uuid"}
		)
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VideoDestinationOutboxEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "video_uuid", nullable = false, columnDefinition = "BINARY(16)")
	private UUID videoUuid;

	@Enumerated(EnumType.STRING)
	@Column(name = "event_type", nullable = false, length = 32)
	private VideoDestinationOutboxEventType eventType;

	@Column(name = "destination_uuid", nullable = false, columnDefinition = "BINARY(16)")
	private UUID destinationUuid;

	@Column(name = "payload_json", nullable = false, columnDefinition = "JSON")
	private String payloadJson;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 16)
	private VideoProcessingOutboxStatus status;

	@Column(name = "attempt_count", nullable = false)
	private int attemptCount;

	@Column(name = "next_attempt_at", nullable = false)
	private LocalDateTime nextAttemptAt;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	@Column(name = "sent_at")
	private LocalDateTime sentAt;

	@Column(name = "last_error", length = 512)
	private String lastError;

	public static VideoDestinationOutboxEntity pending(
			UUID videoUuid,
			VideoDestinationOutboxEventType eventType,
			UUID destinationUuid,
			String payloadJson,
			LocalDateTime now
	) {
		VideoDestinationOutboxEntity entity = new VideoDestinationOutboxEntity();
		entity.videoUuid = videoUuid;
		entity.eventType = eventType;
		entity.destinationUuid = destinationUuid;
		entity.payloadJson = payloadJson;
		entity.status = VideoProcessingOutboxStatus.PENDING;
		entity.attemptCount = 0;
		entity.nextAttemptAt = now;
		entity.createdAt = now;
		return entity;
	}

	public void markSent(LocalDateTime now) {
		this.status = VideoProcessingOutboxStatus.SENT;
		this.sentAt = now;
		this.lastError = null;
	}

	public void scheduleRetry(LocalDateTime nextAttemptAt, String errorMessage) {
		this.attemptCount++;
		this.nextAttemptAt = nextAttemptAt;
		this.lastError = truncate(errorMessage, 512);
	}

	public void markFailed(String errorMessage) {
		this.status = VideoProcessingOutboxStatus.FAILED;
		this.lastError = truncate(errorMessage, 512);
	}

	private static String truncate(String value, int maxLength) {
		if (value == null) {
			return null;
		}
		return value.length() <= maxLength ? value : value.substring(0, maxLength);
	}

}
