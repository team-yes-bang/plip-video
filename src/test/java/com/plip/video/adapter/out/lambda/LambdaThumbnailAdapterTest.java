package com.plip.video.adapter.out.lambda;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.plip.video.global.config.AwsProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import software.amazon.awssdk.services.lambda.model.InvocationType;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LambdaThumbnailAdapterTest {

	private static final String THUMBNAIL_ARN = "arn:aws:lambda:ap-northeast-2:334687118442:function:yes-plip-dev-video-thumbnail";
	private static final UUID VIDEO_UUID = UUID.fromString("0195bbbb-bbbb-7bbb-bbbb-bbbbbbbbbbbb");
	private static final String RAW_S3_KEY = "videos/raw/" + VIDEO_UUID + ".mp4";

	@Mock
	private LambdaClient lambdaClient;

	private LambdaThumbnailAdapter adapter;

	@BeforeEach
	void setUp() {
		adapter = new LambdaThumbnailAdapter(lambdaClient, awsProperties(THUMBNAIL_ARN), new ObjectMapper());
	}

	@Test
	void invokesThumbnailLambdaAsAsyncEvent() throws Exception {
		adapter.invokeThumbnailGeneration(VIDEO_UUID, RAW_S3_KEY);

		ArgumentCaptor<InvokeRequest> captor = ArgumentCaptor.forClass(InvokeRequest.class);
		verify(lambdaClient).invoke(captor.capture());

		InvokeRequest request = captor.getValue();
		assertThat(request.functionName()).isEqualTo(THUMBNAIL_ARN);
		assertThat(request.invocationType()).isEqualTo(InvocationType.EVENT);

		@SuppressWarnings("unchecked")
		Map<String, String> payload = new ObjectMapper().readValue(
				request.payload().asUtf8String(),
				Map.class
		);
		assertThat(payload).containsEntry("videoUuid", VIDEO_UUID.toString());
		assertThat(payload).containsEntry("rawS3Key", RAW_S3_KEY);
	}

	@Test
	void skipsInvokeWhenArnIsBlank() {
		adapter = new LambdaThumbnailAdapter(lambdaClient, awsProperties(" "), new ObjectMapper());

		adapter.invokeThumbnailGeneration(VIDEO_UUID, RAW_S3_KEY);

		verify(lambdaClient, never()).invoke(org.mockito.ArgumentMatchers.any(InvokeRequest.class));
	}

	private AwsProperties awsProperties(String thumbnailArn) {
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
				new AwsProperties.SqsProperties("https://sqs.example.com/queue"),
				new AwsProperties.LambdaProperties(thumbnailArn)
		);
	}
}
