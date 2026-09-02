package com.plip.video.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "plip.storage")
public record StorageProperties(
		/**
		 * Resolved storage mode: {@code s3}, {@code local}, or {@code noop}.
		 * Set via {@code PLIP_STORAGE_TYPE}, or derived from {@code AWS_ENABLED} when unset/{@code auto}.
		 * When {@code local}, AWS is forced off (see {@link StorageEnvironmentPostProcessor}).
		 */
		String type,
		LocalProperties local
) {

	public record LocalProperties(
			String root,
			/** Gateway public origin for browser URLs (no trailing slash), e.g. http://localhost:8000 */
			String publicBaseUrl,
			String tokenSecret
	) {
	}

	public boolean isLocal() {
		return "local".equalsIgnoreCase(type);
	}

	public boolean isS3() {
		return "s3".equalsIgnoreCase(type);
	}

	public boolean isNoop() {
		return "noop".equalsIgnoreCase(type);
	}
}
