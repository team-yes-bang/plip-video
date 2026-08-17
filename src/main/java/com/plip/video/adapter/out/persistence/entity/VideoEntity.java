package com.plip.video.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
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

	@Column(name = "processed_path", length = 255)
	private String processedPath;

	@Column(name = "file_size_byte", nullable = false)
	private long fileSizeByte;

	@Column(name = "thumbnail_image_path", length = 255)
	private String thumbnailImagePath;

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
			String thumbnailImagePath
	) {
		this.videoUuid = videoUuid;
		this.userUuid = userUuid;
		this.caption = caption;
		this.filePath = filePath;
		this.fileSizeByte = fileSizeByte;
		this.thumbnailImagePath = thumbnailImagePath;
	}

	public void softDelete() {
		this.deletedAt = LocalDateTime.now();
	}

	public void updateThumbnailImagePath(String thumbnailImagePath) {
		this.thumbnailImagePath = thumbnailImagePath;
	}

	public void updateProcessedPath(String processedPath) {
		this.processedPath = processedPath;
	}
}
