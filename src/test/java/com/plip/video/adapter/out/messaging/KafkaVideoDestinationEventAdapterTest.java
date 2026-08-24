package com.plip.video.adapter.out.messaging;

import com.plip.video.adapter.out.messaging.kafka.dto.DiaryVideoUploadedKafkaEvent;
import com.plip.video.adapter.out.messaging.kafka.dto.VideoUploadedKafkaEvent;
import com.plip.video.application.port.out.dto.DiaryVideoUploadedMessage;
import com.plip.video.application.port.out.dto.TopicVideoUploadedMessage;
import com.plip.video.global.config.AppKafkaProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class KafkaVideoDestinationEventAdapterTest {

	private static final String VIDEO_UPLOADED_TOPIC = "video.uploaded";
	private static final String DIARY_VIDEO_UPLOADED_TOPIC = "diary.video.uploaded";

	@Mock
	private KafkaTemplate<String, VideoUploadedKafkaEvent> videoUploadedKafkaTemplate;

	@Mock
	private KafkaTemplate<String, DiaryVideoUploadedKafkaEvent> diaryVideoUploadedKafkaTemplate;

	private KafkaVideoDestinationEventAdapter adapter;

	@BeforeEach
	void setUp() {
		AppKafkaProperties properties = new AppKafkaProperties(
				true,
				new AppKafkaProperties.Topics(VIDEO_UPLOADED_TOPIC, DIARY_VIDEO_UPLOADED_TOPIC)
		);
		adapter = new KafkaVideoDestinationEventAdapter(
				videoUploadedKafkaTemplate,
				diaryVideoUploadedKafkaTemplate,
				properties
		);
	}

	@Test
	void publishTopicVideoUploaded_sendsToVideoUploadedTopicWithVideoUuidKey() {
		UUID topicUuid = UUID.fromString("01959999-bbbb-9999-9999-999999999999");
		UUID videoUuid = UUID.fromString("01957777-bbbb-7777-7777-777777777777");
		UUID userUuid = UUID.fromString("01958888-bbbb-8888-8888-888888888888");
		Instant occurredAt = Instant.parse("2026-08-24T06:00:00Z");

		adapter.publishTopicVideoUploaded(new TopicVideoUploadedMessage(
				topicUuid,
				videoUuid,
				userUuid,
				"hello",
				occurredAt
		));

		ArgumentCaptor<VideoUploadedKafkaEvent> captor = ArgumentCaptor.forClass(VideoUploadedKafkaEvent.class);
		verify(videoUploadedKafkaTemplate).send(eq(VIDEO_UPLOADED_TOPIC), eq(videoUuid.toString()), captor.capture());

		VideoUploadedKafkaEvent event = captor.getValue();
		assertThat(event.topicUuid()).isEqualTo(topicUuid);
		assertThat(event.videoUuid()).isEqualTo(videoUuid);
		assertThat(event.userUuid()).isEqualTo(userUuid);
		assertThat(event.caption()).isEqualTo("hello");
		assertThat(event.occurredAt()).isEqualTo(occurredAt);
	}

	@Test
	void publishDiaryVideoUploaded_sendsToDiaryVideoUploadedTopicWithVideoUuidKey() {
		UUID themeUuid = UUID.fromString("0195cccc-bbbb-cccc-cccc-cccccccccccc");
		UUID videoUuid = UUID.fromString("0195aaaa-bbbb-aaaa-aaaa-aaaaaaaaaaaa");
		UUID userUuid = UUID.fromString("0195bbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
		Instant occurredAt = Instant.parse("2026-08-24T06:00:00Z");

		adapter.publishDiaryVideoUploaded(new DiaryVideoUploadedMessage(
				themeUuid,
				videoUuid,
				userUuid,
				"diary caption",
				null,
				occurredAt
		));

		ArgumentCaptor<DiaryVideoUploadedKafkaEvent> captor = ArgumentCaptor.forClass(DiaryVideoUploadedKafkaEvent.class);
		verify(diaryVideoUploadedKafkaTemplate).send(
				eq(DIARY_VIDEO_UPLOADED_TOPIC),
				eq(videoUuid.toString()),
				captor.capture()
		);

		DiaryVideoUploadedKafkaEvent event = captor.getValue();
		assertThat(event.themeUuid()).isEqualTo(themeUuid);
		assertThat(event.videoUuid()).isEqualTo(videoUuid);
		assertThat(event.userUuid()).isEqualTo(userUuid);
		assertThat(event.caption()).isEqualTo("diary caption");
		assertThat(event.thumbnailUrl()).isNull();
		assertThat(event.occurredAt()).isEqualTo(occurredAt);
	}
}
