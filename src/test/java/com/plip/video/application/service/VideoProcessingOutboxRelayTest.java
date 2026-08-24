package com.plip.video.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.plip.video.adapter.out.persistence.entity.VideoProcessingOutboxEntity;
import com.plip.video.adapter.out.persistence.entity.VideoProcessingOutboxEventType;
import com.plip.video.adapter.out.persistence.entity.VideoProcessingOutboxStatus;
import com.plip.video.adapter.out.persistence.repository.VideoProcessingOutboxJpaRepository;
import com.plip.video.application.port.out.ThumbnailLambdaPort;
import com.plip.video.application.port.out.VideoProcessingQueuePort;
import com.plip.video.global.config.VideoProcessingOutboxProperties;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class VideoProcessingOutboxRelayTest {

	private static final UUID VIDEO_UUID = UUID.fromString("0195bbbb-bbbb-7bbb-bbbb-bbbbbbbbbbbb");
	private static final String RAW_S3_KEY = "videos/raw/" + VIDEO_UUID + ".mp4";

	@Mock
	private VideoProcessingOutboxJpaRepository videoProcessingOutboxJpaRepository;

	@Mock
	private ThumbnailLambdaPort thumbnailLambdaPort;

	@Mock
	private VideoProcessingQueuePort videoProcessingQueuePort;

	private VideoProcessingOutboxRelay relay;

	@BeforeEach
	void setUp() {
		relay = new VideoProcessingOutboxRelay(
				videoProcessingOutboxJpaRepository,
				thumbnailLambdaPort,
				videoProcessingQueuePort,
				new VideoProcessingOutboxProperties(20, 5, 5000L, 1000L),
				new ObjectMapper()
		);
	}

	@Test
	void relayPendingEvents_dispatchesThumbnailAndSqsJobs() {
		LocalDateTime now = LocalDateTime.of(2026, 8, 24, 18, 0);
		VideoProcessingOutboxEntity thumbnail = VideoProcessingOutboxEntity.pending(
				VIDEO_UUID,
				VideoProcessingOutboxEventType.THUMBNAIL_INVOKE,
				"{\"rawS3Key\":\"" + RAW_S3_KEY + "\"}",
				now
		);
		VideoProcessingOutboxEntity sqs = VideoProcessingOutboxEntity.pending(
				VIDEO_UUID,
				VideoProcessingOutboxEventType.SQS_ENQUEUE,
				"{\"rawS3Key\":\"" + RAW_S3_KEY + "\",\"caption\":\"hello\",\"overlayTime\":\"12:30\",\"maxDurationSeconds\":5}",
				now
		);
		given(videoProcessingOutboxJpaRepository.findPendingForUpdate(any(), eq(20)))
				.willReturn(List.of(thumbnail, sqs));

		relay.relayPendingEvents();

		verify(thumbnailLambdaPort).invokeThumbnailGeneration(VIDEO_UUID, RAW_S3_KEY);
		verify(videoProcessingQueuePort).enqueueVideoProcessing(VIDEO_UUID, RAW_S3_KEY, "hello", "12:30", 5);
		assert thumbnail.getStatus() == VideoProcessingOutboxStatus.SENT;
		assert sqs.getStatus() == VideoProcessingOutboxStatus.SENT;
	}

}
