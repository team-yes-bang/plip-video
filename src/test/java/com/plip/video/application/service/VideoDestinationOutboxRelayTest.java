package com.plip.video.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.plip.video.adapter.out.persistence.entity.VideoDestinationOutboxEntity;
import com.plip.video.adapter.out.persistence.entity.VideoDestinationOutboxEventType;
import com.plip.video.adapter.out.persistence.entity.VideoProcessingOutboxStatus;
import com.plip.video.adapter.out.persistence.repository.VideoDestinationOutboxJpaRepository;
import com.plip.video.application.port.out.VideoDestinationEventPort;
import com.plip.video.application.port.out.dto.DiaryVideoUploadedMessage;
import com.plip.video.application.port.out.dto.TopicVideoUploadedMessage;
import com.plip.video.global.config.VideoProcessingOutboxProperties;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class VideoDestinationOutboxRelayTest {

	private static final UUID VIDEO_UUID = UUID.fromString("01957777-bbbb-7777-7777-777777777777");
	private static final UUID TOPIC_UUID = UUID.fromString("01959999-bbbb-9999-9999-999999999999");
	private static final UUID USER_UUID = UUID.fromString("01958888-bbbb-8888-8888-888888888888");

	@Mock
	private VideoDestinationOutboxJpaRepository videoDestinationOutboxJpaRepository;

	@Mock
	private VideoDestinationEventPort videoDestinationEventPort;

	private VideoDestinationOutboxRelay relay;

	@BeforeEach
	void setUp() {
		relay = new VideoDestinationOutboxRelay(
				videoDestinationOutboxJpaRepository,
				videoDestinationEventPort,
				new VideoProcessingOutboxProperties(20, 5, 5000L, 1000L),
				new ObjectMapper()
		);
	}

	@Test
	void relayPendingEvents_publishesTopicVideoUploaded() {
		LocalDateTime now = LocalDateTime.of(2026, 8, 24, 18, 0);
		Instant occurredAt = Instant.parse("2026-08-24T09:00:00Z");
		VideoDestinationOutboxEntity event = VideoDestinationOutboxEntity.pending(
				VIDEO_UUID,
				VideoDestinationOutboxEventType.TOPIC_VIDEO_UPLOADED,
				TOPIC_UUID,
				"""
						{
						  "topicUuid":"%s",
						  "videoUuid":"%s",
						  "userUuid":"%s",
						  "caption":"hello",
						  "occurredAt":"%s"
						}
						""".formatted(TOPIC_UUID, VIDEO_UUID, USER_UUID, occurredAt),
				now
		);
		given(videoDestinationOutboxJpaRepository.findPendingForUpdate(any(), eq(20)))
				.willReturn(List.of(event));

		relay.relayPendingEvents();

		ArgumentCaptor<TopicVideoUploadedMessage> captor = ArgumentCaptor.forClass(TopicVideoUploadedMessage.class);
		verify(videoDestinationEventPort).publishTopicVideoUploaded(captor.capture());
		assertThat(captor.getValue().topicUuid()).isEqualTo(TOPIC_UUID);
		assertThat(captor.getValue().videoUuid()).isEqualTo(VIDEO_UUID);
		assertThat(captor.getValue().caption()).isEqualTo("hello");
		assertThat(event.getStatus()).isEqualTo(VideoProcessingOutboxStatus.SENT);
	}

}
