package com.plip.video.global.util;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class VideoUuidGeneratorTest {

	@Test
	void generatesUuidVersion7() {
		UUID uuid = VideoUuidGenerator.generate();

		assertThat(uuid.version()).isEqualTo(7);
	}

	@Test
	void generatesTimeOrderedUuids() throws InterruptedException {
		List<UUID> uuids = new ArrayList<>();
		for (int i = 0; i < 5; i++) {
			uuids.add(VideoUuidGenerator.generate());
			Thread.sleep(1);
		}

		for (int i = 0; i < uuids.size() - 1; i++) {
			assertThat(uuids.get(i)).isLessThan(uuids.get(i + 1));
		}
	}
}
