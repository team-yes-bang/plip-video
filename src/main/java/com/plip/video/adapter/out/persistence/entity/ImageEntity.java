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

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@Entity
@Table(name = "image")
public class ImageEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(length = 100)
	private String title;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private com.plip.video.domain.model.enums.ImageStatus status;

	@Column(name = "img_path", length = 200)
	private String imgPath;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private com.plip.video.domain.model.enums.ImageType type;

	@Column(name = "file_type", length = 50)
	private String fileType;

	@CreatedDate
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@LastModifiedDate
	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	@Column(name = "deleted_at")
	private LocalDateTime deletedAt;

	@Builder
	private ImageEntity(
			String title,
			com.plip.video.domain.model.enums.ImageStatus status,
			String imgPath,
			com.plip.video.domain.model.enums.ImageType type,
			String fileType
	) {
		this.title = title;
		this.status = status;
		this.imgPath = imgPath;
		this.type = type;
		this.fileType = fileType;
	}

	public void softDelete() {
		this.status = com.plip.video.domain.model.enums.ImageStatus.DELETED;
		this.deletedAt = LocalDateTime.now();
	}
}
