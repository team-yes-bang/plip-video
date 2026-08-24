package com.plip.video.application.service;

import com.plip.video.application.port.in.VideoUseCase;
import com.plip.video.application.port.in.dto.VideoCompleteCommand;
import com.plip.video.application.port.in.dto.VideoCompleteResult;
import com.plip.video.application.port.in.dto.VideoDestinationCommand;
import com.plip.video.application.port.in.dto.VideoDestinationKind;
import com.plip.video.application.port.in.dto.VideoDestinationResult;
import com.plip.video.application.port.in.dto.VideoDetailResult;
import com.plip.video.application.port.in.dto.VideoDownloadUrlProcessing;
import com.plip.video.application.port.in.dto.VideoDownloadUrlReady;
import com.plip.video.application.port.in.dto.VideoDownloadUrlResult;
import com.plip.video.application.port.in.dto.VideoOwnershipResult;
import com.plip.video.application.port.in.dto.VideoUploadUrlCommand;
import com.plip.video.application.port.in.dto.VideoUploadUrlResult;
import com.plip.video.application.port.out.StoragePort;
import com.plip.video.application.port.out.StoredObject;
import com.plip.video.application.port.out.VideoAccessPort;
import com.plip.video.application.port.out.VideoDestinationOutboxPort;
import com.plip.video.application.port.out.VideoPersistencePort;
import com.plip.video.application.port.out.VideoProcessingOutboxPort;
import com.plip.video.application.port.out.dto.DiaryVideoUploadedMessage;
import com.plip.video.application.port.out.dto.TopicVideoUploadedMessage;
import com.plip.video.domain.model.Video;
import com.plip.video.global.config.AwsProperties;
import com.plip.video.global.config.VideoProperties;
import com.plip.video.global.util.OverlayTimeFormatter;
import com.plip.video.global.util.VideoUuidGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VideoService implements VideoUseCase {

	private static final String DEFAULT_CONTENT_TYPE = "video/mp4";
	private static final int DOWNLOAD_RETRY_AFTER_SECONDS = 3;
	private static final String DOWNLOAD_PROCESSING_MESSAGE = "다운로드용 영상 가공 중입니다.";
	private static final String DESTINATION_PUBLISHED_STATUS = "PUBLISHED";

	private final VideoPersistencePort videoPersistencePort;
	private final StoragePort storagePort;
	private final VideoProcessingOutboxPort videoProcessingOutboxPort;
	private final VideoDestinationOutboxPort videoDestinationOutboxPort;
	private final VideoAccessPort videoAccessPort;
	private final VideoProperties videoProperties;
	private final AwsProperties awsProperties;

	@Override
	@Transactional(readOnly = true)
	public VideoUploadUrlResult issueUploadUrl(VideoUploadUrlCommand command) {
		String contentType = resolveContentType(command.contentType());
		long contentLengthBytes = requireValidContentLength(command.contentLengthBytes());
		UUID videoUuid = VideoUuidGenerator.generate();
		var presigned = storagePort.createPresignedPutUrl(
				videoUuid,
				contentType,
				contentLengthBytes
		);

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
		return videoPersistencePort.findByVideoUuid(command.videoUuid())
				.map(existing -> completeWhenAlreadyExists(existing, command))
				.orElseGet(() -> completeNewVideo(command));
	}

	@Override
	@Transactional
	public VideoDestinationResult publishDestination(VideoDestinationCommand command) {
		if (command.userUuid() == null) {
			throw new IllegalArgumentException("userUuid is required");
		}
		validateDestinationFields(command);

		Video video = findVideoOrThrow(command.videoUuid());
		if (!video.getUserUuid().equals(command.userUuid())) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Video owner mismatch");
		}

		String caption = normalizeCaption(command.caption());
		if (caption == null) {
			caption = video.getCaption();
		}
		Instant occurredAt = Instant.now();

		if (command.kind() == VideoDestinationKind.TOPIC) {
			videoDestinationOutboxPort.enqueueTopicVideoUploaded(new TopicVideoUploadedMessage(
					command.topicUuid(),
					command.videoUuid(),
					command.userUuid(),
					caption,
					occurredAt
			));
		} else {
			videoDestinationOutboxPort.enqueueDiaryVideoUploaded(new DiaryVideoUploadedMessage(
					command.themeUuid(),
					command.videoUuid(),
					command.userUuid(),
					caption,
					null,
					occurredAt
			));
		}

		return new VideoDestinationResult(command.videoUuid(), DESTINATION_PUBLISHED_STATUS);
	}

	@Override
	@Transactional(readOnly = true)
	public VideoDetailResult getVideo(UUID videoUuid, UUID actorUuid) {
		Video video = findVideoOrThrow(videoUuid);
		requireViewAccess(video, actorUuid);

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
	public VideoDownloadUrlResult getDownloadUrl(UUID videoUuid, UUID actorUuid) {
		Video video = findVideoOrThrow(videoUuid);
		requireViewAccess(video, actorUuid);

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
	@Transactional(readOnly = true)
	public VideoOwnershipResult getOwnership(UUID videoUuid) {
		Video video = findVideoOrThrow(videoUuid);
		return new VideoOwnershipResult(video.getVideoUuid(), video.getUserUuid());
	}

	@Override
	@Transactional
	public void updateThumbnail(UUID videoUuid, String thumbnailS3Key) {
		String normalizedPath = normalizeCallbackPath(thumbnailS3Key, "thumbnailS3Key", awsProperties.s3().imagePrefix());
		videoPersistencePort.updateThumbnailPath(videoUuid, normalizedPath)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Video not found: " + videoUuid));
	}

	@Override
	@Transactional
	public void updateProcessed(UUID videoUuid, String processedS3Key, Integer durationSeconds) {
		validateDurationSeconds(durationSeconds);
		String normalizedPath = normalizeCallbackPath(
				processedS3Key,
				"processedS3Key",
				awsProperties.s3().processedVideoPrefix()
		);
		videoPersistencePort.updateProcessedPath(videoUuid, normalizedPath)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Video not found: " + videoUuid));
	}

	private VideoCompleteResult completeWhenAlreadyExists(Video existing, VideoCompleteCommand command) {
		if (!existing.getUserUuid().equals(command.userUuid())) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Video owner mismatch");
		}
		return toCompleteResult(existing, false);
	}

	private VideoCompleteResult completeNewVideo(VideoCompleteCommand command) {
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

		Video saved;
		try {
			saved = videoPersistencePort.save(video);
		} catch (DataIntegrityViolationException exception) {
			Video raced = videoPersistencePort.findByVideoUuid(command.videoUuid())
					.orElseThrow(() -> exception);
			return completeWhenAlreadyExists(raced, command);
		}

		String overlayTime = OverlayTimeFormatter.formatKstHhMm(saved.getCreatedAt());
		videoProcessingOutboxPort.enqueueProcessingJobs(
				saved.getVideoUuid(),
				rawS3Key,
				saved.getCaption(),
				overlayTime,
				videoProperties.maxDurationSeconds()
		);
		return toCompleteResult(saved, true);
	}

	private VideoCompleteResult toCompleteResult(Video video, boolean newlyCreated) {
		return new VideoCompleteResult(
				video.getVideoUuid(),
				video.getCaption(),
				video.getCreatedAt(),
				OverlayTimeFormatter.formatKstHhMm(video.getCreatedAt()),
				newlyCreated
		);
	}

	private void requireViewAccess(Video video, UUID actorUuid) {
		if (actorUuid == null) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
		}
		if (video.getUserUuid().equals(actorUuid)) {
			return;
		}
		if (videoAccessPort.canView(actorUuid, video.getVideoUuid())) {
			return;
		}
		throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Video access denied");
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
		if (sizeBytes <= 0) {
			throw new IllegalArgumentException("Video file is empty");
		}
		if (sizeBytes > videoProperties.maxFileSizeBytes()) {
			throw new IllegalArgumentException("Video file exceeds max size");
		}
	}

	private long requireValidContentLength(Long contentLengthBytes) {
		if (contentLengthBytes == null) {
			throw new IllegalArgumentException("contentLengthBytes is required");
		}
		validateFileSize(contentLengthBytes);
		return contentLengthBytes;
	}

	private void validateDurationSeconds(Integer durationSeconds) {
		if (durationSeconds == null) {
			throw new IllegalArgumentException("durationSeconds is required");
		}
		if (durationSeconds < 0) {
			throw new IllegalArgumentException("durationSeconds must be >= 0");
		}
		if (durationSeconds > videoProperties.maxDurationSeconds()) {
			throw new IllegalArgumentException(
					"Video duration exceeds max of " + videoProperties.maxDurationSeconds() + " seconds"
			);
		}
	}

	private String normalizeCaption(String caption) {
		if (caption == null || caption.isBlank()) {
			return null;
		}
		String trimmed = caption.trim();
		if (trimmed.length() > 100) {
			throw new IllegalArgumentException("caption must be 100 characters or less");
		}
		return trimmed;
	}

	private String normalizeCallbackPath(String path, String fieldName, String requiredPrefix) {
		if (path == null || path.isBlank()) {
			throw new IllegalArgumentException(fieldName + " is required");
		}
		String normalized = path.trim();
		if (normalized.startsWith("s3://") || normalized.startsWith("/") || normalized.contains("..")) {
			throw new IllegalArgumentException(fieldName + " must be a safe relative S3 path");
		}
		if (requiredPrefix != null && !requiredPrefix.isBlank() && !normalized.startsWith(requiredPrefix)) {
			throw new IllegalArgumentException(fieldName + " must start with " + requiredPrefix);
		}
		return normalized;
	}

	private void validateDestinationFields(VideoDestinationCommand command) {
		if (command.kind() == null) {
			throw new IllegalArgumentException("kind is required");
		}
		if (command.kind() == VideoDestinationKind.TOPIC) {
			if (command.topicUuid() == null) {
				throw new IllegalArgumentException("topicUuid is required when kind is TOPIC");
			}
			return;
		}
		if (command.kind() == VideoDestinationKind.DIARY && command.themeUuid() == null) {
			throw new IllegalArgumentException("themeUuid is required when kind is DIARY");
		}
	}
}
