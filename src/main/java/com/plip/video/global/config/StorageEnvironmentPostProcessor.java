package com.plip.video.global.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.HashMap;
import java.util.Map;

/**
 * Resolves {@code plip.storage.type} and forces {@code plip.aws.enabled=false} when type is {@code local}.
 * <p>
 * {@code PLIP_STORAGE_TYPE} / {@code plip.storage.type}: {@code s3} | {@code local} | {@code noop} | {@code auto} (default).
 * When unset or {@code auto}, type is derived from {@code AWS_ENABLED} / {@code plip.aws.enabled}
 * ({@code true} → {@code s3}, else {@code noop}).
 */
public class StorageEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

	public static final String PROPERTY_SOURCE_NAME = "plipStorageModeOverrides";

	@Override
	public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
		String configured = firstNonBlank(
				environment.getProperty("PLIP_STORAGE_TYPE"),
				environment.getProperty("plip.storage.type")
		);
		String type = configured == null || configured.isBlank() || "auto".equalsIgnoreCase(configured.trim())
				? deriveFromAws(environment)
				: configured.trim().toLowerCase();

		Map<String, Object> overrides = new HashMap<>();
		overrides.put("plip.storage.type", type);

		if ("local".equals(type)) {
			// Local disk mode: AWS S3/SQS/Lambda must stay off.
			overrides.put("plip.aws.enabled", "false");
		}

		environment.getPropertySources().addFirst(new MapPropertySource(PROPERTY_SOURCE_NAME, overrides));
	}

	private static String deriveFromAws(ConfigurableEnvironment environment) {
		String aws = firstNonBlank(
				environment.getProperty("AWS_ENABLED"),
				environment.getProperty("plip.aws.enabled")
		);
		boolean enabled = aws != null && Boolean.parseBoolean(aws.trim());
		return enabled ? "s3" : "noop";
	}

	private static String firstNonBlank(String a, String b) {
		if (a != null && !a.isBlank()) {
			return a;
		}
		if (b != null && !b.isBlank()) {
			return b;
		}
		return null;
	}

	@Override
	public int getOrder() {
		return Ordered.HIGHEST_PRECEDENCE + 10;
	}
}
