package com.plip.video.global.config;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "plip.aws", name = "enabled", havingValue = "true")
public class AwsPropertiesValidator {

	private final AwsProperties awsProperties;

	public AwsPropertiesValidator(AwsProperties awsProperties) {
		this.awsProperties = awsProperties;
	}

	@PostConstruct
	void validateResolvedConfiguration() {
		requireResolved("plip.aws.s3.raw-bucket", awsProperties.s3().rawBucket());
		requireResolved("plip.aws.s3.processed-bucket", awsProperties.s3().processedBucket());
		requireResolved("plip.aws.region", awsProperties.region());
	}

	private static void requireResolved(String propertyName, String value) {
		if (value == null || value.isBlank()) {
			throw new IllegalStateException(
					propertyName + " is empty. Export the matching env var or set a default in application-local.yml.");
		}
		if (value.contains("${")) {
			throw new IllegalStateException(
					propertyName + " contains an unresolved placeholder (" + value + "). "
							+ "Spring Boot does not load .env automatically — export AWS_S3_RAW_BUCKET etc. before bootRun, "
							+ "or rely on ./gradlew bootRun which reads back/plip-video/.env.");
		}
	}
}
