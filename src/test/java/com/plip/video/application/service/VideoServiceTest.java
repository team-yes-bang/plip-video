package com.plip.video.application.service;

import com.plip.video.application.port.in.dto.VideoCompleteCommand;
import com.plip.video.application.port.in.dto.VideoDestinationCommand;
import com.plip.video.application.port.in.dto.VideoDestinationKind;
import com.plip.video.application.port.in.dto.VideoDownloadUrlProcessing;
import com.plip.video.application.port.in.dto.VideoDownloadUrlReady;
import com.plip.video.application.port.in.dto.VideoUploadUrlCommand;
import com.plip.video.application.port.out.PresignedUploadUrl;
import com.plip.video.application.port.out.StoragePort;
import com.plip.video.application.port.out.StoredObject;
import com.plip.video.application.port.out.VideoAccessPort;
import com.plip.video.application.port.out.VideoDestinationOutboxPort;
import com.plip.video.application.port.out.VideoPersistencePort;
import com.plip.video.application.port.out.dto.DiaryVideoUploadedMessage;
import com.plip.video.application.port.out.dto.TopicVideoUploadedMessage;
import com.plip.video.application.port.out.VideoProcessingOutboxPort;
import com.plip.video.domain.model.Video;
import com.plip.video.global.config.AwsProperties;
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

import org.springframework.web.server.ResponseStatusException;
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
	private VideoProcessingOutboxPort videoProcessingOutboxPort;

	@Mock
	private VideoDestinationOutboxPort videoDestinationOutboxPort;

	@Mock
	private VideoAccessPort videoAccessPort;

	@Mock
	private VideoProperties videoProperties;

	@Mock
	private AwsProperties awsProperties;

	@InjectMocks
	private VideoService videoService;

	private UUID userUuid;
	private AwsProperties.S3Properties s3Properties;

	@BeforeEach
	void setUp() {
		userUuid = UUID.fromString("0195aaaa-bbbb-7ccc-dddd-eeeeeeeeeeee");
		s3Properties = new AwsProperties.S3Properties(
				"raw",
				"processed",
				"videos/raw/",
				"videos/processed/",
				"images/",
				"thumbnail/",
				"https://cdn.example"
		);
		given(videoProperties.allowedContentTypes()).willReturn(List.of("video/mp4", "video/quicktime"));
		given(videoProperties.maxFileSizeBytes()).willReturn(52_428_800L);
		given(videoProperties.maxDurationSeconds()).willReturn(5);
		given(awsProperties.s3()).willReturn(s3Properties);
	}

	@Test
	void issueUploadUrlDoesNotPersistVideo() {
		given(storagePort.createPresignedPutUrl(any(UUID.class), eq("video/mp4"), eq(1_024L)))
				.willAnswer(invocation -> {
					UUID videoUuid = invocation.getArgument(0);
					String rawS3Key = "videos/raw/" + videoUuid + ".mp4";
					return new PresignedUploadUrl(rawS3Key, "https://example/upload", Instant.parse("2026-08-17T04:00:00Z"));
				});

		var result = videoService.issueUploadUrl(new VideoUploadUrlCommand(userUuid, null, 1_024L));

		assertThat(result.videoUuid()).isNotNull();
		assertThat(result.rawS3Key()).endsWith(".mp4");
		assertThat(result.uploadUrl()).contains("example");
		verify(videoPersistencePort, never()).save(any());
	}

	@Test
	void issueUploadUrlRejectsOversizeContentLength() {
		assertThatThrownBy(() -> videoService.issueUploadUrl(
				new VideoUploadUrlCommand(userUuid, "video/mp4", 52_428_801L)
		))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("exceeds max size");
	}

	@Test
	void issueUploadUrlRequiresContentLength() {
		assertThatThrownBy(() -> videoService.issueUploadUrl(
				new VideoUploadUrlCommand(userUuid, "video/mp4", null)
		))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("contentLengthBytes is required");
	}

	@Test
	void completeRejectsOversizeFile() {
		UUID videoUuid = UUID.fromString("0195bbbb-bbbb-7bbb-bbbb-bbbbbbbbbbbb");
		String rawS3Key = "videos/raw/" + videoUuid + ".mp4";
		given(videoPersistencePort.findByVideoUuid(videoUuid)).willReturn(Optional.empty());
		given(storagePort.buildRawS3Key(videoUuid)).willReturn(rawS3Key);
		given(storagePort.headRawObject(rawS3Key)).willReturn(new StoredObject(rawS3Key, 60_000_000L));

		assertThatThrownBy(() -> videoService.complete(new VideoCompleteCommand(videoUuid, userUuid, null)))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Video file exceeds max size");
		verify(videoProcessingOutboxPort, never()).enqueueProcessingJobs(any(), any(), any(), any(), any(Integer.class));
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
		assertThat(result.newlyCreated()).isTrue();

		verify(videoProcessingOutboxPort).enqueueProcessingJobs(videoUuid, rawS3Key, "hello", "12:30", 5);

		ArgumentCaptor<Video> videoCaptor = ArgumentCaptor.forClass(Video.class);
		verify(videoPersistencePort).save(videoCaptor.capture());
		assertThat(videoCaptor.getValue().getThumbnailImagePath()).isNull();
		assertThat(videoCaptor.getValue().getFilePath()).isEqualTo(rawS3Key);
	}

	@Test
	void completeIsIdempotentForSameOwner() {
		UUID videoUuid = UUID.fromString("0195cccc-bbbb-7ccc-cccc-cccccccccccc");
		Video existing = Video.builder()
				.videoUuid(videoUuid)
				.userUuid(userUuid)
				.caption("done")
				.filePath("videos/raw/" + videoUuid + ".mp4")
				.fileSizeByte(2048L)
				.createdAt(LocalDateTime.of(2026, 8, 17, 3, 30, 0))
				.build();
		given(videoPersistencePort.findByVideoUuid(videoUuid)).willReturn(Optional.of(existing));

		var result = videoService.complete(new VideoCompleteCommand(videoUuid, userUuid, null));

		assertThat(result.newlyCreated()).isFalse();
		verify(videoProcessingOutboxPort, never()).enqueueProcessingJobs(any(), any(), any(), any(), any(Integer.class));
	}

	@Test
	void completeRejectsDuplicateOwnerMismatch() {
		UUID videoUuid = UUID.fromString("0195cccc-bbbb-7ccc-cccc-cccccccccccc");
		given(videoPersistencePort.findByVideoUuid(videoUuid))
				.willReturn(Optional.of(Video.builder()
						.videoUuid(videoUuid)
						.userUuid(UUID.randomUUID())
						.filePath("videos/raw/x.mp4")
						.fileSizeByte(1L)
						.build()));

		assertThatThrownBy(() -> videoService.complete(new VideoCompleteCommand(videoUuid, userUuid, null)))
				.isInstanceOf(ResponseStatusException.class)
				.hasMessageContaining("owner mismatch");
	}

	@Test
	void getVideoReturnsFeedDetail() {
		UUID videoUuid = UUID.fromString("0195dddd-bbbb-7ddd-dddd-dddddddddddd");
		String rawS3Key = "videos/raw/" + videoUuid + ".mp4";
		String thumbnailPath = "thumbnail/" + videoUuid + ".jpg";

		given(videoPersistencePort.findByVideoUuid(videoUuid)).willReturn(Optional.of(
				Video.builder()
						.videoUuid(videoUuid)
						.userUuid(userUuid)
						.caption("feed caption")
						.filePath(rawS3Key)
						.processedPath("videos/processed/" + videoUuid + ".mp4")
						.fileSizeByte(4096L)
						.thumbnailImagePath(thumbnailPath)
						.createdAt(LocalDateTime.of(2026, 8, 17, 3, 30, 0))
						.build()
		));
		given(storagePort.createPresignedRawPlaybackUrl(rawS3Key)).willReturn("https://example/playback");
		given(storagePort.resolvePublicUrl(thumbnailPath)).willReturn("https://cdn.example/" + thumbnailPath);

		var result = videoService.getVideo(videoUuid, userUuid);

		assertThat(result.videoUuid()).isEqualTo(videoUuid);
		assertThat(result.userUuid()).isEqualTo(userUuid);
		assertThat(result.caption()).isEqualTo("feed caption");
		assertThat(result.rawPlaybackUrl()).isEqualTo("https://example/playback");
		assertThat(result.thumbnailUrl()).contains("cdn.example");
		assertThat(result.overlayTime()).isEqualTo("12:30");
		assertThat(result.downloadReady()).isTrue();
	}

	@Test
	void getVideoReturnsNullThumbnailBeforeLambdaCallback() {
		UUID videoUuid = UUID.fromString("0195eeee-bbbb-7eee-eeee-eeeeeeeeeeee");
		String rawS3Key = "videos/raw/" + videoUuid + ".mp4";

		given(videoPersistencePort.findByVideoUuid(videoUuid)).willReturn(Optional.of(
				Video.builder()
						.videoUuid(videoUuid)
						.userUuid(userUuid)
						.filePath(rawS3Key)
						.fileSizeByte(2048L)
						.createdAt(LocalDateTime.of(2026, 8, 17, 3, 30, 0))
						.build()
		));
		given(storagePort.createPresignedRawPlaybackUrl(rawS3Key)).willReturn("https://example/playback");
		given(storagePort.resolvePublicUrl(null)).willReturn(null);

		var result = videoService.getVideo(videoUuid, userUuid);

		assertThat(result.thumbnailUrl()).isNull();
		assertThat(result.downloadReady()).isFalse();
	}

	@Test
	void getVideoThrowsNotFoundWhenMissing() {
		UUID videoUuid = UUID.fromString("0195ffff-bbbb-7fff-ffff-ffffffffffff");
		given(videoPersistencePort.findByVideoUuid(videoUuid)).willReturn(Optional.empty());

		assertThatThrownBy(() -> videoService.getVideo(videoUuid, userUuid))
				.isInstanceOf(ResponseStatusException.class)
				.hasMessageContaining("Video not found");
	}

	@Test
	void updateThumbnailUpdatesPath() {
		UUID videoUuid = UUID.fromString("01951111-bbbb-7111-1111-111111111111");
		String thumbnailPath = "thumbnail/" + videoUuid + ".jpg";

		given(videoPersistencePort.updateThumbnailPath(videoUuid, thumbnailPath)).willReturn(Optional.of(
				Video.builder().videoUuid(videoUuid).thumbnailImagePath(thumbnailPath).build()
		));

		videoService.updateThumbnail(videoUuid, thumbnailPath);

		verify(videoPersistencePort).updateThumbnailPath(videoUuid, thumbnailPath);
	}

	@Test
	void updateProcessedUpdatesPath() {
		UUID videoUuid = UUID.fromString("01952222-bbbb-7222-2222-222222222222");
		String processedPath = "videos/processed/" + videoUuid + ".mp4";

		given(videoPersistencePort.updateProcessedPath(videoUuid, processedPath)).willReturn(Optional.of(
				Video.builder().videoUuid(videoUuid).processedPath(processedPath).build()
		));

		videoService.updateProcessed(videoUuid, processedPath, 4);

		verify(videoPersistencePort).updateProcessedPath(videoUuid, processedPath);
	}

	@Test
	void updateProcessedRejectsOverMaxDuration() {
		UUID videoUuid = UUID.fromString("01952222-bbbb-7222-2222-222222222223");

		assertThatThrownBy(() -> videoService.updateProcessed(
				videoUuid,
				"videos/processed/" + videoUuid + ".mp4",
				6
		))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("duration exceeds max");
	}

	@Test
	void updateProcessedRejectsMissingDuration() {
		UUID videoUuid = UUID.fromString("01952222-bbbb-7222-2222-222222222224");

		assertThatThrownBy(() -> videoService.updateProcessed(
				videoUuid,
				"videos/processed/" + videoUuid + ".mp4",
				null
		))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("durationSeconds is required");
	}

	@Test
	void updateThumbnailThrowsNotFoundWhenMissing() {
		UUID videoUuid = UUID.fromString("01953333-bbbb-7333-3333-333333333333");
		given(videoPersistencePort.updateThumbnailPath(eq(videoUuid), any())).willReturn(Optional.empty());

		assertThatThrownBy(() -> videoService.updateThumbnail(videoUuid, "thumbnail/x.jpg"))
				.isInstanceOf(ResponseStatusException.class)
				.hasMessageContaining("Video not found");
	}

	@Test
	void getDownloadUrlReturnsProcessingWhenNotReady() {
		UUID videoUuid = UUID.fromString("01954444-bbbb-7444-4444-444444444444");

		given(videoPersistencePort.findByVideoUuid(videoUuid)).willReturn(Optional.of(
				Video.builder()
						.videoUuid(videoUuid)
						.userUuid(userUuid)
						.filePath("videos/raw/" + videoUuid + ".mp4")
						.fileSizeByte(2048L)
						.createdAt(LocalDateTime.of(2026, 8, 17, 3, 30, 0))
						.build()
		));

		var result = videoService.getDownloadUrl(videoUuid, userUuid);

		assertThat(result).isInstanceOf(VideoDownloadUrlProcessing.class);
		var processing = (VideoDownloadUrlProcessing) result;
		assertThat(processing.videoUuid()).isEqualTo(videoUuid);
		assertThat(processing.retryAfterSeconds()).isEqualTo(3);
		assertThat(processing.message()).isEqualTo("다운로드용 영상 가공 중입니다.");
	}

	@Test
	void getDownloadUrlReturnsCloudFrontUrlWhenReady() {
		UUID videoUuid = UUID.fromString("01955555-bbbb-7555-5555-555555555555");
		String processedPath = "videos/processed/" + videoUuid + ".mp4";

		given(videoPersistencePort.findByVideoUuid(videoUuid)).willReturn(Optional.of(
				Video.builder()
						.videoUuid(videoUuid)
						.userUuid(userUuid)
						.filePath("videos/raw/" + videoUuid + ".mp4")
						.processedPath(processedPath)
						.fileSizeByte(4096L)
						.createdAt(LocalDateTime.of(2026, 8, 17, 3, 30, 0))
						.build()
		));
		given(storagePort.resolvePublicUrl(processedPath)).willReturn("https://cdn.example/" + processedPath);

		var result = videoService.getDownloadUrl(videoUuid, userUuid);

		assertThat(result).isInstanceOf(VideoDownloadUrlReady.class);
		var ready = (VideoDownloadUrlReady) result;
		assertThat(ready.videoUuid()).isEqualTo(videoUuid);
		assertThat(ready.downloadUrl()).contains("cdn.example");
	}

	@Test
	void getDownloadUrlThrowsNotFoundWhenMissing() {
		UUID videoUuid = UUID.fromString("01956666-bbbb-7666-6666-666666666666");
		given(videoPersistencePort.findByVideoUuid(videoUuid)).willReturn(Optional.empty());

		assertThatThrownBy(() -> videoService.getDownloadUrl(videoUuid, userUuid))
				.isInstanceOf(ResponseStatusException.class)
				.hasMessageContaining("Video not found");
	}

	@Test
	void publishDestinationTopicReturnsPublished() {
		UUID videoUuid = UUID.fromString("01957777-bbbb-7777-7777-777777777777");
		UUID userUuid = UUID.fromString("01958888-bbbb-8888-8888-888888888888");
		UUID topicUuid = UUID.fromString("01959999-bbbb-9999-9999-999999999999");
		given(videoPersistencePort.findByVideoUuid(videoUuid)).willReturn(Optional.of(
				Video.builder()
						.videoUuid(videoUuid)
						.userUuid(userUuid)
						.caption("stored")
						.filePath("videos/raw/" + videoUuid + ".mp4")
						.fileSizeByte(1024L)
						.createdAt(LocalDateTime.of(2026, 8, 24, 12, 0, 0))
						.build()
		));

		var result = videoService.publishDestination(new VideoDestinationCommand(
				videoUuid,
				userUuid,
				VideoDestinationKind.TOPIC,
				topicUuid,
				null,
				null,
				null
		));

		assertThat(result.videoUuid()).isEqualTo(videoUuid);
		assertThat(result.status()).isEqualTo("PUBLISHED");

		ArgumentCaptor<TopicVideoUploadedMessage> captor = ArgumentCaptor.forClass(TopicVideoUploadedMessage.class);
		verify(videoDestinationOutboxPort).enqueueTopicVideoUploaded(captor.capture());
		assertThat(captor.getValue().topicUuid()).isEqualTo(topicUuid);
		assertThat(captor.getValue().videoUuid()).isEqualTo(videoUuid);
		assertThat(captor.getValue().userUuid()).isEqualTo(userUuid);
		assertThat(captor.getValue().caption()).isEqualTo("stored");
	}

	@Test
	void publishDestinationDiaryUsesRequestCaption() {
		UUID videoUuid = UUID.fromString("0195aaaa-bbbb-aaaa-aaaa-aaaaaaaaaaaa");
		UUID userUuid = UUID.fromString("0195bbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
		UUID themeUuid = UUID.fromString("0195cccc-bbbb-cccc-cccc-cccccccccccc");
		given(videoPersistencePort.findByVideoUuid(videoUuid)).willReturn(Optional.of(
				Video.builder()
						.videoUuid(videoUuid)
						.userUuid(userUuid)
						.caption(null)
						.filePath("videos/raw/" + videoUuid + ".mp4")
						.fileSizeByte(1024L)
						.createdAt(LocalDateTime.of(2026, 8, 24, 12, 0, 0))
						.build()
		));

		videoService.publishDestination(new VideoDestinationCommand(
				videoUuid,
				userUuid,
				VideoDestinationKind.DIARY,
				null,
				themeUuid,
				null,
				"  overlay caption  "
		));

		ArgumentCaptor<DiaryVideoUploadedMessage> captor = ArgumentCaptor.forClass(DiaryVideoUploadedMessage.class);
		verify(videoDestinationOutboxPort).enqueueDiaryVideoUploaded(captor.capture());
		assertThat(captor.getValue().themeUuid()).isEqualTo(themeUuid);
		assertThat(captor.getValue().caption()).isEqualTo("overlay caption");
		assertThat(captor.getValue().thumbnailUrl()).isNull();
	}

	@Test
	void publishDestinationRejectsOwnerMismatch() {
		UUID videoUuid = UUID.randomUUID();
		UUID ownerUuid = UUID.randomUUID();
		UUID otherUuid = UUID.randomUUID();
		given(videoPersistencePort.findByVideoUuid(videoUuid)).willReturn(Optional.of(
				Video.builder()
						.videoUuid(videoUuid)
						.userUuid(ownerUuid)
						.filePath("videos/raw/x.mp4")
						.fileSizeByte(1L)
						.createdAt(LocalDateTime.now())
						.build()
		));

		assertThatThrownBy(() -> videoService.publishDestination(new VideoDestinationCommand(
				videoUuid,
				otherUuid,
				VideoDestinationKind.TOPIC,
				UUID.randomUUID(),
				null,
				null,
				null
		))).isInstanceOf(ResponseStatusException.class);
	}

	@Test
	void publishDestinationRequiresTopicUuidForTopicKind() {
		assertThatThrownBy(() -> videoService.publishDestination(new VideoDestinationCommand(
				UUID.randomUUID(),
				UUID.randomUUID(),
				VideoDestinationKind.TOPIC,
				null,
				null,
				null,
				null
		))).isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("topicUuid");
	}
}
