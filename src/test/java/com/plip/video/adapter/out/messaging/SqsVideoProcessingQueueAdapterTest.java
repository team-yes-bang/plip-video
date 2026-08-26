package com.plip.video.adapter.out.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.plip.video.global.config.AwsProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SqsVideoProcessingQueueAdapterTest {

	private static final String QUEUE_URL =
			"https://sqs.ap-northeast-2.amazonaws.com/334687118442/yes-plip-dev-video-processing";
	private static final UUID VIDEO_UUID = UUID.fromString("0195bbbb-bbbb-7bbb-bbbb-bbbbbbbbbbbb");
	private static final String RAW_S3_KEY = "videos/raw/" + VIDEO_UUID + ".mp4";

	@Mock
	private SqsClient sqsClient;

	private SqsVideoProcessingQueueAdapter adapter;

	@BeforeEach
	void setUp() {
		adapter = new SqsVideoProcessingQueueAdapter(sqsClient, awsProperties(QUEUE_URL), new ObjectMapper());
	}

	@Test
	void enqueuesVideoProcessingMessageWithV1Schema() throws Exception {
		adapter.enqueueVideoProcessing(VIDEO_UUID, RAW_S3_KEY, "hello", "12:30", 5);

		ArgumentCaptor<SendMessageRequest> captor = ArgumentCaptor.forClass(SendMessageRequest.class);
		verify(sqsClient).sendMessage(captor.capture());

		SendMessageRequest request = captor.getValue();
		assertThat(request.queueUrl()).isEqualTo(QUEUE_URL);

		@SuppressWarnings("unchecked")
		Map<String, Object> body = new ObjectMapper().readValue(request.messageBody(), Map.class);
		assertThat(body).containsEntry("videoUuid", VIDEO_UUID.toString());
		assertThat(body).containsEntry("rawS3Key", RAW_S3_KEY);
		assertThat(body).containsEntry("caption", "hello");
		assertThat(body).containsEntry("overlayTime", "12:30");
		assertThat(body).containsEntry("maxDurationSeconds", 5);
	}

	@Test
	void omitsBlankCaptionAndOverlayTime() throws Exception {
		adapter.enqueueVideoProcessing(VIDEO_UUID, RAW_S3_KEY, "  ", null, 5);

		ArgumentCaptor<SendMessageRequest> captor = ArgumentCaptor.forClass(SendMessageRequest.class);
		verify(sqsClient).sendMessage(captor.capture());

		@SuppressWarnings("unchecked")
		Map<String, Object> body = new ObjectMapper().readValue(captor.getValue().messageBody(), Map.class);
		assertThat(body).containsKeys("videoUuid", "rawS3Key");
		assertThat(body).doesNotContainKeys("caption", "overlayTime");
	}

	@Test
	void skipsEnqueueWhenQueueUrlIsBlank() {
		adapter = new SqsVideoProcessingQueueAdapter(sqsClient, awsProperties(" "), new ObjectMapper());

		adapter.enqueueVideoProcessing(VIDEO_UUID, RAW_S3_KEY, "hello", "12:30", 5);

		verify(sqsClient, never()).sendMessage(org.mockito.ArgumentMatchers.any(SendMessageRequest.class));
	}

	private AwsProperties awsProperties(String queueUrl) {
		return new AwsProperties(
				true,
				"ap-northeast-2",
				10800,
				new AwsProperties.S3Properties(
						"raw-bucket",
						"processed-bucket",
						"videos/raw/",
						"videos/processed/",
						"images/",
						"thumbnail/",
						"https://cdn.example.com"
				),
				new AwsProperties.SqsProperties(queueUrl),
				new AwsProperties.LambdaProperties("arn:aws:lambda:ap-northeast-2:123:function:thumb")
		);
	}
}
