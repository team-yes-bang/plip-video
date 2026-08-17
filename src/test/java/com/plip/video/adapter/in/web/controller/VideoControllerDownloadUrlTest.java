package com.plip.video.adapter.in.web.controller;

import com.plip.video.application.port.in.VideoUseCase;
import com.plip.video.application.port.in.dto.VideoDownloadUrlProcessing;
import com.plip.video.application.port.in.dto.VideoDownloadUrlReady;
import com.plip.video.global.config.InternalProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(VideoController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(VideoControllerDownloadUrlTest.TestConfig.class)
class VideoControllerDownloadUrlTest {

	private static final UUID VIDEO_UUID = UUID.fromString("0195dddd-bbbb-7ddd-dddd-dddddddddddd");

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private VideoUseCase videoUseCase;

	@Test
	void getDownloadUrlReturns202WhenProcessing() throws Exception {
		given(videoUseCase.getDownloadUrl(VIDEO_UUID)).willReturn(
				new VideoDownloadUrlProcessing(VIDEO_UUID, 3, "다운로드용 영상 가공 중입니다.")
		);

		mockMvc.perform(get("/api/videos/{videoUuid}/download-url", VIDEO_UUID))
				.andExpect(status().isAccepted())
				.andExpect(header().string("Retry-After", "3"))
				.andExpect(jsonPath("$.status").value("PROCESSING"))
				.andExpect(jsonPath("$.videoUuid").value(VIDEO_UUID.toString()))
				.andExpect(jsonPath("$.retryAfterSeconds").value(3))
				.andExpect(jsonPath("$.message").value("다운로드용 영상 가공 중입니다."));
	}

	@Test
	void getDownloadUrlReturns200WhenReady() throws Exception {
		given(videoUseCase.getDownloadUrl(VIDEO_UUID)).willReturn(
				new VideoDownloadUrlReady(VIDEO_UUID, "https://cdn.example/videos/processed/test.mp4")
		);

		mockMvc.perform(get("/api/videos/{videoUuid}/download-url", VIDEO_UUID))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.videoUuid").value(VIDEO_UUID.toString()))
				.andExpect(jsonPath("$.downloadUrl").value("https://cdn.example/videos/processed/test.mp4"));
	}

	@TestConfiguration
	static class TestConfig {

		@Bean
		InternalProperties internalProperties() {
			return new InternalProperties("test-internal-key");
		}
	}
}
