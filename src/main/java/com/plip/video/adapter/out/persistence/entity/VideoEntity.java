package com.plip.video.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@Entity
@Table(name = "video")
public class VideoEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "video_uuid", nullable = false, columnDefinition = "BINARY(16)")
	private UUID videoUuid;

	@Column(name = "user_uuid", nullable = false, columnDefinition = "BINARY(16)")
	private UUID userUuid;

	@Column(length = 100)
	private String caption;

	@Column(name = "file_path", nullable = false, length = 255)
	private String filePath;

	@Column(name = "file_size_byte", nullable = false)
	private long fileSizeByte;

	@Column(name = "thumbnail_image_path", nullable = false, length = 255)
	private String thumbnailImagePath;

	@Column(name = "recorded_at", nullable = false)
	private LocalDateTime recordedAt;

	@Enumerated(EnumType.STRING)
	@Column(name = "processing_status", nullable = false, length = 20)
	private com.plip.video.domain.model.enums.VideoProcessingStatus processingStatus;

	@Column(name = "processed_file_path", length = 255)
	private String processedFilePath;

	@CreatedDate
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@LastModifiedDate
	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	@Column(name = "deleted_at")
	private LocalDateTime deletedAt;

	@Builder
	private VideoEntity(
			UUID videoUuid,
			UUID userUuid,
			String caption,
			String filePath,
			long fileSizeByte,
			String thumbnailImagePath,
			LocalDateTime recordedAt,
			com.plip.video.domain.model.enums.VideoProcessingStatus processingStatus,
			String processedFilePath
	) {
		this.videoUuid = videoUuid;
		this.userUuid = userUuid;
		this.caption = caption;
		this.filePath = filePath;
		this.fileSizeByte = fileSizeByte;
		this.thumbnailImagePath = thumbnailImagePath;
		this.recordedAt = recordedAt;
		this.processingStatus = processingStatus;
		this.processedFilePath = processedFilePath;
	}

	public void markProcessing() {
		this.processingStatus = com.plip.video.domain.model.enums.VideoProcessingStatus.PROCESSING;
	}

	public void markReady(String processedFilePath) {
		this.processingStatus = com.plip.video.domain.model.enums.VideoProcessingStatus.READY;
		this.processedFilePath = processedFilePath;
	}

	public void markFailed() {
		this.processingStatus = com.plip.video.domain.model.enums.VideoProcessingStatus.FAILED;
	}

	public void softDelete() {
		this.deletedAt = LocalDateTime.now();
	}
}
