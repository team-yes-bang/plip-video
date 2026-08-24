package com.plip.video.global.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.topic")
public class TopicInternalProperties {

	private String internalBaseUrl = "http://localhost:8084";

	private String internalApiKey = "";
}
