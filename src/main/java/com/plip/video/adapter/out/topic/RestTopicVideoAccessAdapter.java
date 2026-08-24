package com.plip.video.adapter.out.topic;

import com.plip.video.application.port.out.VideoAccessPort;
import com.plip.video.global.config.TopicInternalProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.UUID;

/**
 * Topic membership-based playback. Owner access is decided in VideoService without this call.
 * When topic API is missing/unreachable, returns false (fail-closed) so uploader playback still works.
 */
@Slf4j
@Component
@Profile("!test")
@RequiredArgsConstructor
public class RestTopicVideoAccessAdapter implements VideoAccessPort {

	private static final String ACCESS_PATH = "/internal/v1/videos/{videoUuid}/access/{userUuid}";

	private final RestClient topicInternalRestClient;
	private final TopicInternalProperties topicInternalProperties;

	@Override
	public boolean canView(UUID actorUuid, UUID videoUuid) {
		if (actorUuid == null || videoUuid == null) {
			return false;
		}
		if (topicInternalProperties.getInternalApiKey() == null
				|| topicInternalProperties.getInternalApiKey().isBlank()) {
			return false;
		}
		try {
			topicInternalRestClient.head()
					.uri(ACCESS_PATH, videoUuid, actorUuid)
					.retrieve()
					.onStatus(HttpStatusCode::isError, (request, response) -> {
						throw new RestClientResponseException(
								"Topic video access denied",
								response.getStatusCode().value(),
								response.getStatusText(),
								response.getHeaders(),
								null,
								null
						);
					})
					.toBodilessEntity();
			return true;
		} catch (RestClientResponseException exception) {
			if (exception.getStatusCode().value() == 404 || exception.getStatusCode().value() == 403) {
				return false;
			}
			log.warn("Topic video access check failed status={} videoUuid={}",
					exception.getStatusCode().value(), videoUuid);
			return false;
		} catch (ResourceAccessException exception) {
			log.warn("Topic video access unreachable videoUuid={}", videoUuid, exception);
			return false;
		}
	}
}
