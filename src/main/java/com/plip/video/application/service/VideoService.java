package com.plip.video.application.service;

import com.plip.video.application.port.in.VideoUseCase;
import com.plip.video.application.port.in.dto.VideoCompleteCommand;
import com.plip.video.application.port.in.dto.VideoCompleteResult;
import com.plip.video.application.port.in.dto.VideoDetailResult;
import com.plip.video.application.port.in.dto.VideoDownloadUrlProcessing;
import com.plip.video.application.port.in.dto.VideoDownloadUrlReady;
import com.plip.video.application.port.in.dto.VideoDownloadUrlResult;
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

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VideoService implements VideoUseCase {

	private static final String DEFAULT_CONTENT_TYPE = "video/mp4";
	private static final int DOWNLOAD_RETRY_AFTER_SECONDS = 3;
	private static final String DOWNLOAD_PROCESSING_MESSAGE = "다운로드용 영상 가공 중입니다.";

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
		Video video = findVideoOrThrow(videoUuid);

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
	@Transactional(readOnly = true)
	public VideoDownloadUrlResult getDownloadUrl(UUID videoUuid) {
		Video video = findVideoOrThrow(videoUuid);

		if (!video.isDownloadReady()) {
			return new VideoDownloadUrlProcessing(
					video.getVideoUuid(),
					DOWNLOAD_RETRY_AFTER_SECONDS,
					DOWNLOAD_PROCESSING_MESSAGE
			);
		}

		return new VideoDownloadUrlReady(
				video.getVideoUuid(),
				storagePort.resolvePublicUrl(video.getProcessedPath())
		);
	}

	@Override
	@Transactional
	public void updateThumbnail(UUID videoUuid, String thumbnailS3Key) {
		String normalizedPath = normalizeRelativePath(thumbnailS3Key, "thumbnailS3Key");
		videoPersistencePort.updateThumbnailPath(videoUuid, normalizedPath)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Video not found: " + videoUuid));
	}

	@Override
	@Transactional
	public void updateProcessed(UUID videoUuid, String processedS3Key) {
		String normalizedPath = normalizeRelativePath(processedS3Key, "processedS3Key");
		videoPersistencePort.updateProcessedPath(videoUuid, normalizedPath)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Video not found: " + videoUuid));
	}

	private Video findVideoOrThrow(UUID videoUuid) {
		return videoPersistencePort.findByVideoUuid(videoUuid)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Video not found: " + videoUuid));
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

	private String normalizeCaption(String caption) {
		if (caption == null || caption.isBlank()) {
			return null;
		}
		return caption.trim();
	}

	private String normalizeRelativePath(String path, String fieldName) {
		if (path == null || path.isBlank()) {
			throw new IllegalArgumentException(fieldName + " is required");
		}
		String normalized = path.trim();
		if (normalized.startsWith("s3://") || normalized.startsWith("/")) {
			throw new IllegalArgumentException(fieldName + " must be a relative S3 path");
		}
		return normalized;
	}
}
