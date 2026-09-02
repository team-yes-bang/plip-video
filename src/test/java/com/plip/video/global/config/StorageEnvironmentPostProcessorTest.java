package com.plip.video.global.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

class StorageEnvironmentPostProcessorTest {

	private final StorageEnvironmentPostProcessor processor = new StorageEnvironmentPostProcessor();

	@Test
	void localForcesAwsOff() {
		MockEnvironment environment = new MockEnvironment()
				.withProperty("PLIP_STORAGE_TYPE", "local")
				.withProperty("AWS_ENABLED", "true");

		processor.postProcessEnvironment(environment, new SpringApplication());

		assertThat(environment.getProperty("plip.storage.type")).isEqualTo("local");
		assertThat(environment.getProperty("plip.aws.enabled")).isEqualTo("false");
	}

	@Test
	void autoDerivesS3WhenAwsEnabled() {
		MockEnvironment environment = new MockEnvironment()
				.withProperty("AWS_ENABLED", "true");

		processor.postProcessEnvironment(environment, new SpringApplication());

		assertThat(environment.getProperty("plip.storage.type")).isEqualTo("s3");
	}

	@Test
	void autoDerivesNoopWhenAwsDisabled() {
		MockEnvironment environment = new MockEnvironment()
				.withProperty("AWS_ENABLED", "false");

		processor.postProcessEnvironment(environment, new SpringApplication());

		assertThat(environment.getProperty("plip.storage.type")).isEqualTo("noop");
	}
}
