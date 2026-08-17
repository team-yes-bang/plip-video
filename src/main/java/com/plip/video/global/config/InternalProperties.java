package com.plip.video.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "plip.internal")
public record InternalProperties(
		String apiKey
) {
}
