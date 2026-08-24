package com.plip.video.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "plip.gateway.hmac")
public record GatewayHmacProperties(
		String secret,
		Long maxSkewSeconds
) {

	public boolean enabled() {
		return secret != null && !secret.isBlank();
	}

	public long maxSkewSecondsOrDefault() {
		return maxSkewSeconds == null || maxSkewSeconds <= 0 ? 300L : maxSkewSeconds;
	}

}
