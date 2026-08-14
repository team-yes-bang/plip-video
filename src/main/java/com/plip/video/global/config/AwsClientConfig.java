package com.plip.video.global.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.sqs.SqsClient;

@Configuration
@ConditionalOnProperty(prefix = "plip.aws", name = "enabled", havingValue = "true")
public class AwsClientConfig {

	@Bean
	public S3Client s3Client(AwsProperties awsProperties) {
		return S3Client.builder()
				.region(Region.of(awsProperties.region()))
				.build();
	}

	@Bean
	public SqsClient sqsClient(AwsProperties awsProperties) {
		return SqsClient.builder()
				.region(Region.of(awsProperties.region()))
				.build();
	}
}
