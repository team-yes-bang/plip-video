package com.plip.video.adapter.out.lambda;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.plip.video.application.port.out.ThumbnailLambdaPort;
import com.plip.video.global.config.AwsProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import software.amazon.awssdk.services.lambda.model.InvocationType;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "plip.aws", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class LambdaThumbnailAdapter implements ThumbnailLambdaPort {

	private final LambdaClient lambdaClient;
	private final AwsProperties awsProperties;
	private final ObjectMapper objectMapper;

	@Override
	public void invokeThumbnailGeneration(UUID videoUuid, String rawS3Key) {
		String functionArn = awsProperties.lambda().thumbnailFunctionArn();
		if (functionArn == null || functionArn.isBlank()) {
			log.warn("Thumbnail Lambda ARN is not configured — skip invoke for {}", videoUuid);
			return;
		}

		try {
			String payload = objectMapper.writeValueAsString(Map.of(
					"videoUuid", videoUuid.toString(),
					"rawS3Key", rawS3Key
			));

			InvokeRequest request = InvokeRequest.builder()
					.functionName(functionArn)
					.invocationType(InvocationType.EVENT)
					.payload(SdkBytes.fromUtf8String(payload))
					.build();

			lambdaClient.invoke(request);
			log.info("Invoked thumbnail lambda for videoUuid={}", videoUuid);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("Failed to serialize thumbnail lambda payload", e);
		}
	}
}
