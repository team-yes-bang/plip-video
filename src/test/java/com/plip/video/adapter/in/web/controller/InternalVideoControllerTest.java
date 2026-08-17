package com.plip.video.adapter.in.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.plip.video.application.port.in.VideoUseCase;
import com.plip.video.global.config.InternalApiKeyFilter;
import com.plip.video.global.config.InternalProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static com.plip.video.global.config.InternalApiKeyFilter.HEADER_NAME;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InternalVideoController.class)
@Import({InternalApiKeyFilter.class, InternalVideoControllerTest.TestConfig.class})
class InternalVideoControllerTest {

	private static final String API_KEY = "test-internal-key";
	private static final UUID VIDEO_UUID = UUID.fromString("0195aaaa-bbbb-7ccc-dddd-eeeeeeeeeeee");

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private VideoUseCase videoUseCase;

	@Test
	void updateThumbnailRequiresApiKey() throws Exception {
		mockMvc.perform(patch("/internal/videos/{videoUuid}/thumbnail", VIDEO_UUID)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(
								new com.plip.video.adapter.in.web.dto.InternalUpdateThumbnailRequest(
										"images/thumbnails/test.jpg"
								))))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void updateThumbnailWithValidApiKey() throws Exception {
		mockMvc.perform(patch("/internal/videos/{videoUuid}/thumbnail", VIDEO_UUID)
						.header(HEADER_NAME, API_KEY)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(
								new com.plip.video.adapter.in.web.dto.InternalUpdateThumbnailRequest(
										"images/thumbnails/test.jpg"
								))))
				.andExpect(status().isNoContent());

		verify(videoUseCase).updateThumbnail(eq(VIDEO_UUID), eq("images/thumbnails/test.jpg"));
	}

	@Test
	void updateProcessedWithValidApiKey() throws Exception {
		mockMvc.perform(patch("/internal/videos/{videoUuid}/processed", VIDEO_UUID)
						.header(HEADER_NAME, API_KEY)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(
								new com.plip.video.adapter.in.web.dto.InternalUpdateProcessedRequest(
										"videos/processed/test.mp4"
								))))
				.andExpect(status().isNoContent());

		verify(videoUseCase).updateProcessed(eq(VIDEO_UUID), eq("videos/processed/test.mp4"));
	}

	@TestConfiguration
	static class TestConfig {

		@Bean
		InternalProperties internalProperties() {
			return new InternalProperties(API_KEY);
		}
	}
}
