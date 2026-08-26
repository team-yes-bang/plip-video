package com.plip.video.adapter.out.messaging;

import com.plip.video.application.port.out.VideoDestinationEventPort;
import com.plip.video.application.port.out.dto.DiaryVideoUploadedMessage;
import com.plip.video.application.port.out.dto.TopicVideoUploadedMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "app.kafka", name = "enabled", havingValue = "false", matchIfMissing = true)
public class NoOpVideoDestinationEventAdapter implements VideoDestinationEventPort {

	@Override
	public void publishTopicVideoUploaded(TopicVideoUploadedMessage message) {
		log.info(
				"Kafka disabled — stub publish video.uploaded: topicUuid={} videoUuid={} userUuid={}",
				message.topicUuid(),
				message.videoUuid(),
				message.userUuid()
		);
	}

	@Override
	public void publishDiaryVideoUploaded(DiaryVideoUploadedMessage message) {
		log.info(
				"Kafka disabled — stub publish diary.video.uploaded: themeUuid={} videoUuid={} userUuid={}",
				message.themeUuid(),
				message.videoUuid(),
				message.userUuid()
		);
	}
}
