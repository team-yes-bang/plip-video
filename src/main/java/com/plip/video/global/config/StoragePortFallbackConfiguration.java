package com.plip.video.global.config;

import com.plip.video.adapter.out.storage.NoOpStorageAdapter;
import com.plip.video.application.port.out.StoragePort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Ensures a {@link StoragePort} exists when {@code plip.storage.type} is unresolved (e.g. {@code auto}
 * before post-processing) or misconfigured in tests.
 */
@Configuration
public class StoragePortFallbackConfiguration {

	@Bean
	@ConditionalOnMissingBean(StoragePort.class)
	StoragePort fallbackStoragePort(AwsProperties awsProperties) {
		return new NoOpStorageAdapter(awsProperties);
	}
}
