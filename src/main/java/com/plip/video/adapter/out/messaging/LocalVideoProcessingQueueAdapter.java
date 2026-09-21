package com.plip.video.adapter.out.messaging;

import com.plip.video.adapter.out.storage.LocalObjectStorageService;
import com.plip.video.application.port.out.VideoPersistencePort;
import com.plip.video.application.port.out.VideoProcessingQueuePort;
import com.plip.video.global.config.AwsProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "plip.storage", name = "type", havingValue = "local")
@RequiredArgsConstructor
public class LocalVideoProcessingQueueAdapter implements VideoProcessingQueuePort {

	private final LocalObjectStorageService localObjectStorageService;
	private final AwsProperties awsProperties;
	private final VideoPersistencePort videoPersistencePort;

	@Override
	public void enqueueVideoProcessing(
			UUID videoUuid,
			String rawS3Key,
			String caption,
			String overlayTime,
			int maxDurationSeconds
	) {
		String processedKey = awsProperties.s3().processedVideoPrefix() + videoUuid + ".mp4";
		try {
			localObjectStorageService.copyObject(rawS3Key, processedKey);
		} catch (IOException exception) {
			throw new IllegalStateException(
					"Failed to copy raw video to processed path for " + videoUuid,
					exception
			);
		}

		videoPersistencePort.updateProcessedPath(videoUuid, processedKey)
				.orElseThrow(() -> new ResponseStatusException(
						HttpStatus.NOT_FOUND,
						"Video not found: " + videoUuid
				));

		log.info(
				"Local video processing completed: videoUuid={}, rawKey={}, processedKey={}",
				videoUuid,
				rawS3Key,
				processedKey
		);
	}
}
