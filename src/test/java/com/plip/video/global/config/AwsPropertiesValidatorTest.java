package com.plip.video.global.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class AwsPropertiesValidatorTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withUserConfiguration(TestConfig.class)
			.withBean(AwsPropertiesValidator.class);

	@Test
	void acceptsResolvedBucketNames() {
		contextRunner
				.withPropertyValues(
						"plip.aws.enabled=true",
						"plip.aws.region=ap-northeast-2",
						"plip.aws.presigned-url-ttl-seconds=10800",
						"plip.aws.s3.raw-bucket=yes-plip-dev-media-334687118442-raw",
						"plip.aws.s3.processed-bucket=yes-plip-dev-media-334687118442-processed",
						"plip.aws.s3.cdn-base-url=https://cdn.example.com",
						"plip.aws.sqs.video-processing-queue-url=https://sqs.example.com/queue",
						"plip.aws.lambda.thumbnail-function-arn=arn:aws:lambda:ap-northeast-2:123:function:thumb")
				.run(context -> assertThat(context).hasNotFailed());
	}

	@Test
	void rejectsUnresolvedRawBucketPlaceholder() {
		contextRunner
				.withPropertyValues(
						"plip.aws.enabled=true",
						"plip.aws.region=ap-northeast-2",
						"plip.aws.presigned-url-ttl-seconds=10800",
						"plip.aws.s3.raw-bucket=${AWS_S3_RAW_BUCKET}",
						"plip.aws.s3.processed-bucket=test-processed",
						"plip.aws.s3.cdn-base-url=https://cdn.example.com",
						"plip.aws.sqs.video-processing-queue-url=https://sqs.example.com/queue",
						"plip.aws.lambda.thumbnail-function-arn=arn:aws:lambda:ap-northeast-2:123:function:thumb")
				.run(context -> {
					assertThat(context).hasFailed();
					assertThat(context.getStartupFailure()).isInstanceOf(BeanCreationException.class);
					assertThat(context.getStartupFailure().getCause()).hasMessageContaining("unresolved placeholder");
				});
	}

	@EnableConfigurationProperties(AwsProperties.class)
	static class TestConfig {
	}
}
