package com.plip.video.adapter.in.web;

import com.plip.video.adapter.in.web.controller.VideoController;
import com.plip.video.support.WebMvcSecurityTestConfig;
import com.plip.video.application.port.in.VideoUseCase;
import com.plip.video.application.port.in.dto.VideoUploadUrlResult;
import com.plip.video.global.config.InternalProperties;
import com.plip.video.global.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(VideoController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, VideoControllerAuthTest.TestConfig.class, WebMvcSecurityTestConfig.class})
class VideoControllerAuthTest {

	private static final UUID USER_UUID = UUID.fromString("0195eeee-bbbb-7eee-eeee-eeeeeeeeeeee");

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private VideoUseCase videoUseCase;

	@Test
	void issueUploadUrlRequiresAuthentication() throws Exception {
		mockMvc.perform(post("/api/v1/videos/upload-url").param("contentLengthBytes", "1024"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void issueUploadUrlWithAuthenticatedActor() throws Exception {
		given(videoUseCase.issueUploadUrl(any())).willReturn(
				new VideoUploadUrlResult(
						UUID.randomUUID(),
						"videos/raw/test.mp4",
						"https://example/upload",
						Instant.parse("2026-08-17T04:00:00Z")
				)
		);

		mockMvc.perform(post("/api/v1/videos/upload-url")
						.param("contentLengthBytes", "1024")
						.with(AuthenticatedActorTestSupport.authenticated(USER_UUID)))
				.andExpect(status().isCreated());
	}

	@TestConfiguration
	static class TestConfig {

		@Bean
		InternalProperties internalProperties() {
			return new InternalProperties("test-internal-key");
		}
	}
}
