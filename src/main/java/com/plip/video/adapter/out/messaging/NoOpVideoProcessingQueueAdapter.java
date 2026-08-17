package com.plip.video.adapter.out.messaging;

import com.plip.video.application.port.out.VideoProcessingQueuePort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "plip.aws", name = "enabled", havingValue = "false", matchIfMissing = true)
public class NoOpVideoProcessingQueueAdapter implements VideoProcessingQueuePort {

	@Override
	public void enqueueVideoProcessing(UUID videoUuid, String rawS3Key, String caption, String overlayTime) {
		log.warn(
				"AWS disabled — stub enqueue video processing: videoUuid={}, rawS3Key={}, caption={}, overlayTime={}",
				videoUuid,
				rawS3Key,
				caption,
				overlayTime
		);
	}
}
