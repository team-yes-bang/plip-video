package com.plip.video.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "plip.video")
public record VideoProperties(
		int maxDurationSeconds,
		List<String> allowedContentTypes,
		long maxFileSizeBytes
) {
}
