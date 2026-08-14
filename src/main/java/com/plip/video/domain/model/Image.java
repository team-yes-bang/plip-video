package com.plip.video.domain.model;

import com.plip.video.domain.model.enums.ImageStatus;
import com.plip.video.domain.model.enums.ImageType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class Image {

	private final Long id;
	private final String title;
	private final ImageStatus status;
	private final String imgPath;
	private final ImageType type;
	private final String fileType;
	private final LocalDateTime createdAt;
	private final LocalDateTime updatedAt;
	private final LocalDateTime deletedAt;
}
