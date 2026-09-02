package com.plip.video.adapter.out.storage;

import com.plip.video.global.config.StorageProperties;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalObjectTokenServiceTest {

	private final LocalObjectTokenService tokenService = new LocalObjectTokenService(
			new StorageProperties(
					"local",
					new StorageProperties.LocalProperties("/tmp", "http://localhost:8000", "test-secret")
			)
	);

	@Test
	void signAndVerify() {
		long expires = 4_102_444_800L;
		String key = "videos/raw/sample.mp4";
		String sig = tokenService.sign("PUT", key, expires);

		tokenService.verifyOrThrow("PUT", key, Long.toString(expires), sig);
	}

	@Test
	void rejectsWrongMethod() {
		long expires = 4_102_444_800L;
		String key = "thumbnail/sample.jpg";
		String sig = tokenService.sign("PUT", key, expires);

		assertThatThrownBy(() -> tokenService.verifyOrThrow("GET", key, Long.toString(expires), sig))
				.isInstanceOf(ResponseStatusException.class);
	}

	@Test
	void rejectsExpiredToken() {
		String key = "videos/raw/old.mp4";
		long expires = 1L;
		String sig = tokenService.sign("GET", key, expires);

		assertThatThrownBy(() -> tokenService.verifyOrThrow("GET", key, Long.toString(expires), sig))
				.isInstanceOf(ResponseStatusException.class)
				.hasMessageContaining("expired");
	}
}
