package com.plip.video.application.service;

import com.plip.video.application.port.in.VideoUseCase;
import com.plip.video.application.port.in.dto.VideoCompleteCommand;
import com.plip.video.application.port.in.dto.VideoCompleteResult;
import com.plip.video.application.port.in.dto.VideoDetailResult;
import com.plip.video.application.port.in.dto.VideoRegisterCommand;
import com.plip.video.application.port.in.dto.VideoRegisterResult;
import com.plip.video.application.port.in.dto.VideoUploadUrlCommand;
import com.plip.video.application.port.in.dto.VideoUploadUrlResult;
import com.plip.video.application.port.out.StoragePort;
import com.plip.video.application.port.out.StoredObject;
import com.plip.video.application.port.out.ThumbnailLambdaPort;
import com.plip.video.application.port.out.VideoPersistencePort;
import com.plip.video.application.port.out.VideoProcessingQueuePort;
import com.plip.video.domain.model.Video;
import com.plip.video.global.config.VideoProperties;
import com.plip.video.global.util.OverlayTimeFormatter;
import com.plip.video.global.util.VideoUuidGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VideoService implements VideoUseCase {

	private static final String DEFAULT_CONTENT_TYPE = "video/mp4";

	private final VideoPersistencePort videoPersistencePort;
	private final StoragePort storagePort;
	private final VideoProcessingQueuePort videoProcessingQueuePort;
	private final ThumbnailLambdaPort thumbnailLambdaPort;
	private final VideoProperties videoProperties;

	@Override
	@Transactional(readOnly = true)
	public VideoUploadUrlResult issueUploadUrl(VideoUploadUrlCommand command) {
		String contentType = resolveContentType(command.contentType());
		UUID videoUuid = VideoUuidGenerator.generate();
		var presigned = storagePort.createPresignedPutUrl(videoUuid, contentType);

		return new VideoUploadUrlResult(
				videoUuid,
				presigned.rawS3Key(),
				presigned.uploadUrl(),
				presigned.expiresAt()
		);
	}

	@Override
	@Transactional
	public VideoCompleteResult complete(VideoCompleteCommand command) {
		if (videoPersistencePort.findByVideoUuid(command.videoUuid()).isPresent()) {
			throw new IllegalArgumentException("Video already completed: " + command.videoUuid());
		}

		String rawS3Key = storagePort.buildRawS3Key(command.videoUuid());
		StoredObject rawObject = storagePort.headRawObject(rawS3Key);
		validateFileSize(rawObject.sizeBytes());

		Video video = Video.builder()
				.videoUuid(command.videoUuid())
				.userUuid(command.userUuid())
				.caption(normalizeCaption(command.caption()))
				.filePath(rawS3Key)
				.fileSizeByte(rawObject.sizeBytes())
				.build();

		Video saved = videoPersistencePort.save(video);
		String overlayTime = OverlayTimeFormatter.formatKstHhMm(saved.getCreatedAt());

		thumbnailLambdaPort.invokeThumbnailGeneration(saved.getVideoUuid(), rawS3Key);
		videoProcessingQueuePort.enqueueVideoProcessing(
				saved.getVideoUuid(),
				rawS3Key,
				saved.getCaption(),
				overlayTime
		);

		return new VideoCompleteResult(
				saved.getVideoUuid(),
				saved.getCaption(),
				saved.getCreatedAt(),
				overlayTime
		);
	}

	@Override
	@Transactional(readOnly = true)
	public VideoDetailResult getVideo(UUID videoUuid) {
		Video video = videoPersistencePort.findByVideoUuid(videoUuid)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Video not found: " + videoUuid));

		return new VideoDetailResult(
				video.getVideoUuid(),
				video.getUserUuid(),
				video.getCaption(),
				video.getCreatedAt(),
				storagePort.createPresignedRawPlaybackUrl(video.getFilePath()),
				storagePort.resolvePublicUrl(video.getThumbnailImagePath()),
				OverlayTimeFormatter.formatKstHhMm(video.getCreatedAt()),
				video.isDownloadReady()
		);
	}

	@Override
	@Transactional
	public VideoRegisterResult register(VideoRegisterCommand command) {
		validateRegistration(command);

		UUID videoUuid = VideoUuidGenerator.generate();
		StoredObject rawVideo = uploadRawVideo(videoUuid, command);
		StoredObject thumbnail = uploadThumbnailPlaceholder(videoUuid, command);

		Video video = Video.builder()
				.videoUuid(videoUuid)
				.userUuid(command.userUuid())
				.caption(normalizeCaption(command.caption()))
				.filePath(rawVideo.relativePath())
				.fileSizeByte(rawVideo.sizeBytes())
				.thumbnailImagePath(thumbnail.relativePath())
				.build();

		Video saved = videoPersistencePort.save(video);
		String overlayTime = OverlayTimeFormatter.formatKstHhMm(saved.getCreatedAt());
		videoProcessingQueuePort.enqueueVideoProcessing(
				saved.getVideoUuid(),
				saved.getFilePath(),
				saved.getCaption(),
				overlayTime
		);

		return new VideoRegisterResult(
				saved.getVideoUuid(),
				saved.getCaption(),
				saved.getCreatedAt(),
				storagePort.resolvePublicUrl(saved.getThumbnailImagePath())
		);
	}

	@Override
	@Transactional(readOnly = true)
	public void requestDownloadProcessing(UUID videoUuid) {
		Video video = videoPersistencePort.findByVideoUuid(videoUuid)
				.orElseThrow(() -> new IllegalArgumentException("Video not found: " + videoUuid));

		String overlayTime = OverlayTimeFormatter.formatKstHhMm(video.getCreatedAt());
		videoProcessingQueuePort.enqueueVideoProcessing(
				videoUuid,
				video.getFilePath(),
				video.getCaption(),
				overlayTime
		);
	}

	private void validateRegistration(VideoRegisterCommand command) {
		if (command.videoFile() == null || command.videoFile().isEmpty()) {
			throw new IllegalArgumentException("Video file is required");
		}
		validateFileSize(command.videoFile().getSize());
		validateContentType(command.videoFile().getContentType());
	}

	private String resolveContentType(String contentType) {
		String resolved = (contentType == null || contentType.isBlank()) ? DEFAULT_CONTENT_TYPE : contentType.trim();
		validateContentType(resolved);
		return resolved;
	}

	private void validateContentType(String contentType) {
		if (contentType != null && !videoProperties.allowedContentTypes().contains(contentType)) {
			throw new IllegalArgumentException("Unsupported content type: " + contentType);
		}
	}

	private void validateFileSize(long sizeBytes) {
		if (sizeBytes > videoProperties.maxFileSizeBytes()) {
			throw new IllegalArgumentException("Video file exceeds max size");
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
