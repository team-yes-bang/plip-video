package com.plip.video.adapter.out.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.plip.video.application.port.out.VideoProcessingQueuePort;
import com.plip.video.global.config.AwsProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "plip.aws", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class SqsVideoProcessingQueueAdapter implements VideoProcessingQueuePort {

	private final SqsClient sqsClient;
	private final AwsProperties awsProperties;
	private final ObjectMapper objectMapper;

	@Override
	public void enqueueVideoProcessing(UUID videoUuid, String rawS3Key, String caption, String overlayTime) {
		String queueUrl = awsProperties.sqs().videoProcessingQueueUrl();
		if (queueUrl == null || queueUrl.isBlank()) {
			log.warn("Video processing SQS queue URL is not configured — skip enqueue for {}", videoUuid);
			return;
		}

		Map<String, Object> body = new LinkedHashMap<>();
		body.put("videoUuid", videoUuid.toString());
		body.put("rawS3Key", rawS3Key);
		if (caption != null && !caption.isBlank()) {
			body.put("caption", caption);
		}
		if (overlayTime != null && !overlayTime.isBlank()) {
			body.put("overlayTime", overlayTime);
		}

		try {
			SendMessageRequest request = SendMessageRequest.builder()
					.queueUrl(queueUrl)
					.messageBody(objectMapper.writeValueAsString(body))
					.build();

			sqsClient.sendMessage(request);
			log.info("Enqueued video processing job for {}", videoUuid);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("Failed to serialize SQS message", e);
		}
	}

	@Override
	public void enqueueForDownloadProcessing(UUID videoUuid, String rawVideoPath) {
		enqueueVideoProcessing(videoUuid, rawVideoPath, null, null);
	}
}
