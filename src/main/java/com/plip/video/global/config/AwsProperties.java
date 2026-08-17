package com.plip.video.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "plip.aws")
public record AwsProperties(
		boolean enabled,
		String region,
		int presignedUrlTtlSeconds,
		S3Properties s3,
		SqsProperties sqs,
		LambdaProperties lambda
) {

	public record S3Properties(
			String rawBucket,
			String processedBucket,
			String rawVideoPrefix,
			String processedVideoPrefix,
			String imagePrefix,
			String cdnBaseUrl
	) {
	}

	public record SqsProperties(
			String videoProcessingQueueUrl
	) {
	}

	public record LambdaProperties(
			String thumbnailFunctionArn
	) {
	}
}
