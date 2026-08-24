package com.plip.video.adapter.in.web.controller;

import com.plip.video.adapter.in.web.AuthenticatedActorTestSupport;
import com.plip.video.support.WebMvcSecurityTestConfig;
import com.plip.video.application.port.in.VideoUseCase;
import com.plip.video.application.port.in.dto.VideoDestinationResult;
import com.plip.video.global.config.InternalProperties;
import com.plip.video.global.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(VideoController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, VideoControllerDestinationTest.TestConfig.class, WebMvcSecurityTestConfig.class})
class VideoControllerDestinationTest {

	private static final UUID VIDEO_UUID = UUID.fromString("0195dddd-bbbb-7ddd-dddd-dddddddddddd");
	private static final UUID USER_UUID = UUID.fromString("0195eeee-bbbb-7eee-eeee-eeeeeeeeeeee");
	private static final UUID TOPIC_UUID = UUID.fromString("0195ffff-bbbb-7fff-ffff-ffffffffffff");

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private VideoUseCase videoUseCase;

	@AfterEach
	void clearSecurityContext() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void publishDestinationReturns202WithHeaderUserUuid() throws Exception {
		given(videoUseCase.publishDestination(any())).willReturn(
				new VideoDestinationResult(VIDEO_UUID, "PUBLISHED")
		);

		mockMvc.perform(post("/api/videos/{videoUuid}/destination", VIDEO_UUID)
						.with(AuthenticatedActorTestSupport.authenticated(USER_UUID))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "kind": "TOPIC",
								  "topicUuid": "%s"
								}
								""".formatted(TOPIC_UUID)))
				.andExpect(status().isAccepted())
				.andExpect(jsonPath("$.videoUuid").value(VIDEO_UUID.toString()))
				.andExpect(jsonPath("$.status").value("PUBLISHED"));
	}

	@Test
	void publishDestinationReturns401WhenUnauthenticated() throws Exception {
		mockMvc.perform(post("/api/videos/{videoUuid}/destination", VIDEO_UUID)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "kind": "TOPIC",
								  "topicUuid": "%s"
								}
								""".formatted(TOPIC_UUID)))
				.andExpect(status().isUnauthorized());
	}

	@TestConfiguration
	static class TestConfig {

		@Bean
		InternalProperties internalProperties() {
			return new InternalProperties("test-internal-key");
		}
	}
}
