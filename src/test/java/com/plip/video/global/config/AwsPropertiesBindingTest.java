package com.plip.video.global.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = AwsPropertiesBindingTest.TestConfig.class)
@TestPropertySource(properties = {
		"plip.aws.enabled=true",
		"plip.aws.region=ap-northeast-2",
		"plip.aws.presigned-url-ttl-seconds=7200",
		"plip.aws.s3.raw-bucket=test-raw-bucket",
		"plip.aws.s3.processed-bucket=test-processed-bucket",
		"plip.aws.s3.cdn-base-url=https://cdn.example.com",
		"plip.aws.sqs.video-processing-queue-url=https://sqs.example.com/queue",
		"plip.aws.lambda.thumbnail-function-arn=arn:aws:lambda:ap-northeast-2:123:function:thumb",
		"plip.internal.api-key=test-internal-key"
})
class AwsPropertiesBindingTest {

	@Autowired
	private AwsProperties awsProperties;

	@Autowired
	private InternalProperties internalProperties;

	@Test
	void bindsAwsAndInternalProperties() {
		assertThat(awsProperties.enabled()).isTrue();
		assertThat(awsProperties.region()).isEqualTo("ap-northeast-2");
		assertThat(awsProperties.presignedUrlTtlSeconds()).isEqualTo(7200);
		assertThat(awsProperties.s3().rawBucket()).isEqualTo("test-raw-bucket");
		assertThat(awsProperties.s3().processedBucket()).isEqualTo("test-processed-bucket");
		assertThat(awsProperties.s3().cdnBaseUrl()).isEqualTo("https://cdn.example.com");
		assertThat(awsProperties.sqs().videoProcessingQueueUrl()).isEqualTo("https://sqs.example.com/queue");
		assertThat(awsProperties.lambda().thumbnailFunctionArn())
				.isEqualTo("arn:aws:lambda:ap-northeast-2:123:function:thumb");
		assertThat(internalProperties.apiKey()).isEqualTo("test-internal-key");
	}

	@EnableConfigurationProperties({AwsProperties.class, InternalProperties.class})
	static class TestConfig {
	}
}
