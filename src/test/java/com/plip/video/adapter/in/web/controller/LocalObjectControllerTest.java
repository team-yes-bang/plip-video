package com.plip.video.adapter.in.web.controller;

import com.plip.video.adapter.out.storage.LocalFilesystemStorageAdapter;
import com.plip.video.adapter.out.storage.LocalObjectPathHelper;
import com.plip.video.adapter.out.storage.LocalObjectTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class LocalObjectControllerTest {

	@Mock
	private LocalFilesystemStorageAdapter storageAdapter;

	@Mock
	private LocalObjectTokenService tokenService;

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.standaloneSetup(new LocalObjectController(storageAdapter, tokenService))
				.build();
	}

	@Test
	void putAndGetRoundTrip(@TempDir Path tempDir) throws Exception {
		String objectKey = "videos/raw/" + UUID.randomUUID() + ".mp4";
		byte[] payload = "hello-video".getBytes();
		long expires = Instant.now().plusSeconds(3600).getEpochSecond();
		String putSig = "put-sig";
		String getSig = "get-sig";
		String encodedKey = LocalObjectPathHelper.encodeObjectKey(objectKey);
		Path stored = tempDir.resolve(objectKey);
		Files.createDirectories(stored.getParent());

		doNothing().when(tokenService).verifyOrThrow(eq("PUT"), eq(objectKey), anyString(), eq(putSig));
		doNothing().when(tokenService).verifyOrThrow(eq("GET"), eq(objectKey), anyString(), eq(getSig));
		when(storageAdapter.resolvePath(objectKey)).thenReturn(stored);

		mockMvc.perform(put("/api/v1/local-objects/" + encodedKey)
						.param("expires", Long.toString(expires))
						.param("sig", putSig)
						.contentType(MediaType.APPLICATION_OCTET_STREAM)
						.content(payload))
				.andExpect(status().isOk());

		assertThat(Files.readAllBytes(stored)).isEqualTo(payload);

		mockMvc.perform(get("/api/v1/local-objects/" + encodedKey)
						.param("expires", Long.toString(expires))
						.param("sig", getSig))
				.andExpect(status().isOk())
				.andExpect(result -> assertThat(result.getResponse().getContentAsByteArray()).isEqualTo(payload));
	}
}
