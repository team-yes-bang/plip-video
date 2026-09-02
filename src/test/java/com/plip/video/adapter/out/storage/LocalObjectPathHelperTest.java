package com.plip.video.adapter.out.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalObjectPathHelperTest {

	@Test
	void rejectsPathTraversal(@TempDir Path root) {
		assertThatThrownBy(() -> LocalObjectPathHelper.resolveStoragePath(root, "../secret.txt"))
				.isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
	}
}
