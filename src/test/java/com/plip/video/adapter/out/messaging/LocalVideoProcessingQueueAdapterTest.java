package com.plip.video.adapter.out.messaging;

import com.plip.video.adapter.out.storage.LocalObjectStorageService;
import com.plip.video.application.port.out.VideoPersistencePort;
import com.plip.video.domain.model.Video;
import com.plip.video.global.config.AwsProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LocalVideoProcessingQueueAdapterTest {

	private static final UUID VIDEO_UUID = UUID.fromString("0195bbbb-bbbb-7bbb-bbbb-bbbbbbbbbbbb");
	private static final String RAW_KEY = "videos/raw/" + VIDEO_UUID + ".mp4";
	private static final String PROCESSED_KEY = "videos/processed/" + VIDEO_UUID + ".mp4";

	@Mock
	private LocalObjectStorageService localObjectStorageService;

	@Mock
	private VideoPersistencePort videoPersistencePort;

	private LocalVideoProcessingQueueAdapter adapter;

	@BeforeEach
	void setUp() {
		AwsProperties awsProperties = new AwsProperties(
				false,
				"ap-northeast-2",
				10800,
				new AwsProperties.S3Properties(
						"",
						"",
						"videos/raw/",
						"videos/processed/",
						"images/",
						"thumbnail/",
						""
				),
				new AwsProperties.SqsProperties(""),
				new AwsProperties.LambdaProperties("")
		);
		adapter = new LocalVideoProcessingQueueAdapter(
				localObjectStorageService,
				awsProperties,
				videoPersistencePort
		);
	}

	@Test
	void enqueueVideoProcessing_copiesRawToProcessedAndUpdatesDb() throws Exception {
		given(videoPersistencePort.updateProcessedPath(eq(VIDEO_UUID), eq(PROCESSED_KEY)))
				.willReturn(Optional.of(Video.builder().videoUuid(VIDEO_UUID).build()));

		adapter.enqueueVideoProcessing(VIDEO_UUID, RAW_KEY, "caption", "12:34", 5);

		verify(localObjectStorageService).copyObject(RAW_KEY, PROCESSED_KEY);
		verify(videoPersistencePort).updateProcessedPath(VIDEO_UUID, PROCESSED_KEY);
	}
}
