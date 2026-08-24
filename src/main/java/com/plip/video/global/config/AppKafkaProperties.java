package com.plip.video.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.kafka")
public record AppKafkaProperties(
		boolean enabled,
		Topics topics
) {

	public AppKafkaProperties {
		if (topics == null) {
			topics = new Topics("video.uploaded", "diary.video.uploaded");
		}
	}

	public record Topics(
			String videoUploaded,
			String diaryVideoUploaded
	) {
	}
}
