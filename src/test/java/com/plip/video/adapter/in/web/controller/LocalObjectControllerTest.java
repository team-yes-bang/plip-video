package com.plip.video.adapter.in.web.controller;

import com.plip.video.adapter.out.storage.LocalObjectReadResult;
import com.plip.video.adapter.out.storage.LocalObjectStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class LocalObjectControllerTest {

	@Mock
	private LocalObjectStorageService localObjectStorageService;

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.standaloneSetup(new LocalObjectController(localObjectStorageService))
				.build();
	}

	@Test
	void putAndGetRoundTrip() throws Exception {
		String objectKey = "videos/raw/" + UUID.randomUUID() + ".mp4";
		byte[] payload = "hello-video".getBytes();
		long expires = Instant.now().plusSeconds(3600).getEpochSecond();
		String putSig = "put-sig";
		String getSig = "get-sig";
		String encodedKey = "videos/raw/" + UUID.randomUUID() + ".mp4";

		doNothing().when(localObjectStorageService).putObject(
				eq("/api/v1/local-objects/" + encodedKey),
				eq(""),
				eq(Long.toString(expires)),
				eq(putSig),
				any()
		);
		when(localObjectStorageService.getObject(
				eq("/api/v1/local-objects/" + encodedKey),
				eq(""),
				eq(Long.toString(expires)),
				eq(getSig)
		)).thenReturn(new LocalObjectReadResult(
				objectKey,
				new org.springframework.core.io.InputStreamResource(new ByteArrayInputStream(payload))
		));

		mockMvc.perform(put("/api/v1/local-objects/" + encodedKey)
						.param("expires", Long.toString(expires))
						.param("sig", putSig)
						.contentType(MediaType.APPLICATION_OCTET_STREAM)
						.content(payload))
				.andExpect(status().isOk());

		mockMvc.perform(get("/api/v1/local-objects/" + encodedKey)
						.param("expires", Long.toString(expires))
						.param("sig", getSig))
				.andExpect(status().isOk());
	}
}
