package com.plip.video.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "plip.video.outbox")
public record VideoProcessingOutboxProperties(
		Integer relayBatchSize,
		Integer maxAttempts,
		Long relayIntervalMs,
		Long retryBackoffMs
) {

	public int relayBatchSizeOrDefault() {
		return relayBatchSize == null || relayBatchSize <= 0 ? 20 : relayBatchSize;
	}

	public int maxAttemptsOrDefault() {
		return maxAttempts == null || maxAttempts <= 0 ? 5 : maxAttempts;
	}

	public long relayIntervalMsOrDefault() {
		return relayIntervalMs == null || relayIntervalMs <= 0 ? 5000L : relayIntervalMs;
	}

	public long retryBackoffMsOrDefault() {
		return retryBackoffMs == null || retryBackoffMs <= 0 ? 1000L : retryBackoffMs;
	}

}
