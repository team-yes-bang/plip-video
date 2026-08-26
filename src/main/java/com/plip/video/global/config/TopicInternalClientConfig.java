package com.plip.video.global.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
@Profile("!test")
@EnableConfigurationProperties(TopicInternalProperties.class)
public class TopicInternalClientConfig {

	private static final String INTERNAL_API_KEY_HEADER = "X-Internal-Api-Key";

	@Bean
	RestClient topicInternalRestClient(TopicInternalProperties properties) {
		SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
		requestFactory.setConnectTimeout(Duration.ofSeconds(2));
		requestFactory.setReadTimeout(Duration.ofSeconds(3));
		RestClient.Builder builder = RestClient.builder()
				.baseUrl(properties.getInternalBaseUrl())
				.requestFactory(requestFactory);
		if (properties.getInternalApiKey() != null && !properties.getInternalApiKey().isBlank()) {
			builder.defaultRequest(request -> request.header(
					INTERNAL_API_KEY_HEADER,
					properties.getInternalApiKey()
			));
		}
		return builder.build();
	}
}
