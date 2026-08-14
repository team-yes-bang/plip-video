package com.plip.video.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "plip.aws")
public record AwsProperties(
		boolean enabled,
		String region,
		S3Properties s3,
		SqsProperties sqs
) {

	public record S3Properties(
			String bucket,
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
}
