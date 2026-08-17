package com.plip.video.application.service;

import com.plip.video.application.port.in.dto.VideoCompleteCommand;
import com.plip.video.application.port.in.dto.VideoUploadUrlCommand;
import com.plip.video.application.port.out.PresignedUploadUrl;
import com.plip.video.application.port.out.StoragePort;
import com.plip.video.application.port.out.StoredObject;
import com.plip.video.application.port.out.ThumbnailLambdaPort;
import com.plip.video.application.port.out.VideoPersistencePort;
import com.plip.video.application.port.out.VideoProcessingQueuePort;
import com.plip.video.domain.model.Video;
import com.plip.video.global.config.VideoProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoSettings;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class VideoServiceTest {

	@Mock
	private VideoPersistencePort videoPersistencePort;

	@Mock
	private StoragePort storagePort;

	@Mock
	private VideoProcessingQueuePort videoProcessingQueuePort;

	@Mock
	private ThumbnailLambdaPort thumbnailLambdaPort;

	@Mock
	private VideoProperties videoProperties;

	@InjectMocks
	private VideoService videoService;

	private UUID userUuid;

	@BeforeEach
	void setUp() {
		userUuid = UUID.fromString("0195aaaa-bbbb-7ccc-dddd-eeeeeeeeeeee");
		given(videoProperties.allowedContentTypes()).willReturn(List.of("video/mp4", "video/quicktime"));
		given(videoProperties.maxFileSizeBytes()).willReturn(52_428_800L);
	}

	@Test
	void issueUploadUrlDoesNotPersistVideo() {
		given(storagePort.createPresignedPutUrl(any(UUID.class), eq("video/mp4")))
				.willAnswer(invocation -> {
					UUID videoUuid = invocation.getArgument(0);
					String rawS3Key = "videos/raw/" + videoUuid + ".mp4";
					return new PresignedUploadUrl(rawS3Key, "https://example/upload", Instant.parse("2026-08-17T04:00:00Z"));
				});

		var result = videoService.issueUploadUrl(new VideoUploadUrlCommand(userUuid, null));

		assertThat(result.videoUuid()).isNotNull();
		assertThat(result.rawS3Key()).endsWith(".mp4");
		assertThat(result.uploadUrl()).contains("example");
		verify(videoPersistencePort, never()).save(any());
	}

	@Test
	void completePersistsVideoAndTriggersAsyncJobs() {
		UUID videoUuid = UUID.fromString("0195bbbb-bbbb-7bbb-bbbb-bbbbbbbbbbbb");
		String rawS3Key = "videos/raw/" + videoUuid + ".mp4";

		given(videoPersistencePort.findByVideoUuid(videoUuid)).willReturn(Optional.empty());
		given(storagePort.buildRawS3Key(videoUuid)).willReturn(rawS3Key);
		given(storagePort.headRawObject(rawS3Key)).willReturn(new StoredObject(rawS3Key, 2048L));
		given(videoPersistencePort.save(any(Video.class))).willAnswer(invocation -> {
			Video video = invocation.getArgument(0);
			return Video.builder()
					.id(1L)
					.videoUuid(video.getVideoUuid())
					.userUuid(video.getUserUuid())
					.caption(video.getCaption())
					.filePath(video.getFilePath())
					.fileSizeByte(video.getFileSizeByte())
					.createdAt(LocalDateTime.of(2026, 8, 17, 3, 30, 0))
					.build();
		});

		var result = videoService.complete(new VideoCompleteCommand(videoUuid, userUuid, "hello"));

		assertThat(result.videoUuid()).isEqualTo(videoUuid);
		assertThat(result.caption()).isEqualTo("hello");
		assertThat(result.overlayTime()).isEqualTo("12:30");

		verify(thumbnailLambdaPort).invokeThumbnailGeneration(videoUuid, rawS3Key);
		verify(videoProcessingQueuePort).enqueueVideoProcessing(videoUuid, rawS3Key, "hello", "12:30");

		ArgumentCaptor<Video> videoCaptor = ArgumentCaptor.forClass(Video.class);
		verify(videoPersistencePort).save(videoCaptor.capture());
		assertThat(videoCaptor.getValue().getThumbnailImagePath()).isNull();
		assertThat(videoCaptor.getValue().getFilePath()).isEqualTo(rawS3Key);
	}

	@Test
	void completeRejectsDuplicateVideoUuid() {
		UUID videoUuid = UUID.fromString("0195cccc-bbbb-7ccc-cccc-cccccccccccc");
		given(videoPersistencePort.findByVideoUuid(videoUuid))
				.willReturn(Optional.of(Video.builder().videoUuid(videoUuid).build()));

		assertThatThrownBy(() -> videoService.complete(new VideoCompleteCommand(videoUuid, userUuid, null)))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("already completed");
	}
}
