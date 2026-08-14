package com.plip.video.adapter.out.persistence.mapper;

import com.plip.video.adapter.out.persistence.entity.ImageEntity;
import com.plip.video.adapter.out.persistence.entity.VideoEntity;
import com.plip.video.domain.model.Image;
import com.plip.video.domain.model.Video;
import org.springframework.stereotype.Component;

@Component
public class VideoEntityMapper {

	public Video toDomain(VideoEntity entity) {
		return Video.builder()
				.id(entity.getId())
				.videoUuid(entity.getVideoUuid())
				.userUuid(entity.getUserUuid())
				.caption(entity.getCaption())
				.filePath(entity.getFilePath())
				.fileSizeByte(entity.getFileSizeByte())
				.thumbnailImagePath(entity.getThumbnailImagePath())
				.recordedAt(entity.getRecordedAt())
				.processingStatus(entity.getProcessingStatus())
				.processedFilePath(entity.getProcessedFilePath())
				.createdAt(entity.getCreatedAt())
				.updatedAt(entity.getUpdatedAt())
				.deletedAt(entity.getDeletedAt())
				.build();
	}

	public Image toDomain(ImageEntity entity) {
		return Image.builder()
				.id(entity.getId())
				.title(entity.getTitle())
				.status(entity.getStatus())
				.imgPath(entity.getImgPath())
				.type(entity.getType())
				.fileType(entity.getFileType())
				.createdAt(entity.getCreatedAt())
				.updatedAt(entity.getUpdatedAt())
				.deletedAt(entity.getDeletedAt())
				.build();
	}

	public VideoEntity toEntity(Video video) {
		return VideoEntity.builder()
				.videoUuid(video.getVideoUuid())
				.userUuid(video.getUserUuid())
				.caption(video.getCaption())
				.filePath(video.getFilePath())
				.fileSizeByte(video.getFileSizeByte())
				.thumbnailImagePath(video.getThumbnailImagePath())
				.recordedAt(video.getRecordedAt())
				.processingStatus(video.getProcessingStatus())
				.processedFilePath(video.getProcessedFilePath())
				.build();
	}
}
