package com.plip.video.application.service;

import com.plip.video.application.port.in.VideoUseCase;
import com.plip.video.application.port.in.dto.VideoRegisterCommand;
import com.plip.video.application.port.in.dto.VideoRegisterResult;
import com.plip.video.application.port.out.StoragePort;
import com.plip.video.application.port.out.StoredObject;
import com.plip.video.application.port.out.VideoPersistencePort;
import com.plip.video.application.port.out.VideoProcessingQueuePort;
import com.plip.video.domain.model.Video;
import com.plip.video.domain.model.enums.VideoProcessingStatus;
import com.plip.video.global.config.VideoProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VideoService implements VideoUseCase {

	private final VideoPersistencePort videoPersistencePort;
	private final StoragePort storagePort;
	private final VideoProcessingQueuePort videoProcessingQueuePort;
	private final VideoProperties videoProperties;

	@Override
	@Transactional
	public VideoRegisterResult register(VideoRegisterCommand command) {
		validateRegistration(command);

		UUID videoUuid = UUID.randomUUID();
		StoredObject rawVideo = uploadRawVideo(videoUuid, command);
		StoredObject thumbnail = uploadThumbnailPlaceholder(videoUuid, command);

		Video video = Video.builder()
				.videoUuid(videoUuid)
				.userUuid(command.userUuid())
				.caption(normalizeCaption(command.caption()))
				.filePath(rawVideo.relativePath())
				.fileSizeByte(rawVideo.sizeBytes())
				.thumbnailImagePath(thumbnail.relativePath())
				.recordedAt(command.recordedAt())
				.processingStatus(VideoProcessingStatus.PENDING)
				.build();

		Video saved = videoPersistencePort.save(video);
		videoProcessingQueuePort.enqueueForDownloadProcessing(saved.getVideoUuid(), saved.getFilePath());

		return new VideoRegisterResult(
				saved.getVideoUuid(),
				saved.getCaption(),
				saved.getRecordedAt(),
				storagePort.resolvePublicUrl(saved.getThumbnailImagePath()),
				saved.getProcessingStatus()
		);
	}

	@Override
	@Transactional
	public void requestDownloadProcessing(UUID videoUuid) {
		Video video = videoPersistencePort.findByVideoUuid(videoUuid)
				.orElseThrow(() -> new IllegalArgumentException("Video not found: " + videoUuid));

		videoPersistencePort.updateProcessingStatus(videoUuid, VideoProcessingStatus.PROCESSING, null);
		videoProcessingQueuePort.enqueueForDownloadProcessing(videoUuid, video.getFilePath());
	}

	private void validateRegistration(VideoRegisterCommand command) {
		if (command.videoFile() == null || command.videoFile().isEmpty()) {
			throw new IllegalArgumentException("Video file is required");
		}
		if (command.recordedAt() == null) {
			throw new IllegalArgumentException("Recorded time is required");
		}
		if (command.videoFile().getSize() > videoProperties.maxFileSizeBytes()) {
			throw new IllegalArgumentException("Video file exceeds max size");
		}
		String contentType = command.videoFile().getContentType();
		if (contentType != null && !videoProperties.allowedContentTypes().contains(contentType)) {
			throw new IllegalArgumentException("Unsupported content type: " + contentType);
		}
	}

	private StoredObject uploadRawVideo(UUID videoUuid, VideoRegisterCommand command) {
		try {
			return storagePort.uploadRawVideo(
					videoUuid,
					command.videoFile().getInputStream(),
					command.videoFile().getSize(),
					command.videoFile().getContentType()
			);
		} catch (IOException e) {
			throw new IllegalStateException("Failed to read uploaded video", e);
		}
	}

	private StoredObject uploadThumbnailPlaceholder(UUID videoUuid, VideoRegisterCommand command) {
		try {
			return storagePort.uploadThumbnail(
					videoUuid,
					command.videoFile().getInputStream(),
					command.videoFile().getSize(),
					"image/jpeg"
			);
		} catch (IOException e) {
			throw new IllegalStateException("Failed to read video for thumbnail extraction", e);
		}
	}

	private String normalizeCaption(String caption) {
		if (caption == null || caption.isBlank()) {
			return null;
		}
		return caption.trim();
	}
}
