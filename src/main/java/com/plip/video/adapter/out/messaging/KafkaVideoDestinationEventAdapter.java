package com.plip.video.adapter.out.messaging;

import com.plip.video.adapter.out.messaging.kafka.dto.DiaryVideoUploadedKafkaEvent;
import com.plip.video.adapter.out.messaging.kafka.dto.VideoUploadedKafkaEvent;
import com.plip.video.application.port.out.VideoDestinationEventPort;
import com.plip.video.application.port.out.dto.DiaryVideoUploadedMessage;
import com.plip.video.application.port.out.dto.TopicVideoUploadedMessage;
import com.plip.video.global.config.AppKafkaProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.kafka", name = "enabled", havingValue = "true")
public class KafkaVideoDestinationEventAdapter implements VideoDestinationEventPort {

	private final KafkaTemplate<String, VideoUploadedKafkaEvent> videoUploadedKafkaTemplate;
	private final KafkaTemplate<String, DiaryVideoUploadedKafkaEvent> diaryVideoUploadedKafkaTemplate;
	private final AppKafkaProperties appKafkaProperties;

	@Override
	public void publishTopicVideoUploaded(TopicVideoUploadedMessage message) {
		String topic = appKafkaProperties.topics().videoUploaded();
		VideoUploadedKafkaEvent event = new VideoUploadedKafkaEvent(
				message.topicUuid(),
				message.videoUuid(),
				message.userUuid(),
				message.caption(),
				message.occurredAt()
		);
		videoUploadedKafkaTemplate.send(topic, message.videoUuid().toString(), event);
		log.info(
				"Published video.uploaded: topic={} topicUuid={} videoUuid={} userUuid={}",
				topic,
				message.topicUuid(),
				message.videoUuid(),
				message.userUuid()
		);
	}

	@Override
	public void publishDiaryVideoUploaded(DiaryVideoUploadedMessage message) {
		String topic = appKafkaProperties.topics().diaryVideoUploaded();
		DiaryVideoUploadedKafkaEvent event = new DiaryVideoUploadedKafkaEvent(
				message.themeUuid(),
				message.videoUuid(),
				message.userUuid(),
				message.caption(),
				message.thumbnailUrl(),
				message.occurredAt()
		);
		diaryVideoUploadedKafkaTemplate.send(topic, message.videoUuid().toString(), event);
		log.info(
				"Published diary.video.uploaded: topic={} themeUuid={} videoUuid={} userUuid={}",
				topic,
				message.themeUuid(),
				message.videoUuid(),
				message.userUuid()
		);
	}
}
