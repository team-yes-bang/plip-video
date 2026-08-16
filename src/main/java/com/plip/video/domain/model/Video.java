package com.plip.video.domain.model;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class Video {

	private final Long id;
	private final UUID videoUuid;
	private final UUID userUuid;
	private final String caption;
	private final String filePath;
	private final String processedPath;
	private final long fileSizeByte;
	private final String thumbnailImagePath;
	private final LocalDateTime createdAt;
	private final LocalDateTime updatedAt;
	private final LocalDateTime deletedAt;

	public boolean hasCaption() {
		return caption != null && !caption.isBlank();
	}

	public boolean isDownloadReady() {
		return processedPath != null && !processedPath.isBlank();
	}
}
