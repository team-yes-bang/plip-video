package com.plip.video.adapter.out.messaging;

import com.plip.video.application.port.out.VideoProcessingQueuePort;
import com.plip.video.global.config.AwsProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.util.UUID;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "plip.aws", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class SqsVideoProcessingQueueAdapter implements VideoProcessingQueuePort {

	private final SqsClient sqsClient;
	private final AwsProperties awsProperties;

	@Override
	public void enqueueForDownloadProcessing(UUID videoUuid, String rawVideoPath) {
		String body = """
				{"videoUuid":"%s","rawVideoPath":"%s"}
				""".formatted(videoUuid, rawVideoPath).trim();

		SendMessageRequest request = SendMessageRequest.builder()
				.queueUrl(awsProperties.sqs().videoProcessingQueueUrl())
				.messageBody(body)
				.build();

		sqsClient.sendMessage(request);
		log.info("Enqueued video processing job for {}", videoUuid);
	}
}
