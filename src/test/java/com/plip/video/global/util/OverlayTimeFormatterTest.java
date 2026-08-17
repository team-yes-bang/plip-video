package com.plip.video.global.util;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class OverlayTimeFormatterTest {

	@Test
	void formatsCreatedAtAsKstHhMm() {
		LocalDateTime utcCreatedAt = LocalDateTime.of(2026, 8, 17, 3, 30, 0);

		String overlayTime = OverlayTimeFormatter.formatKstHhMm(utcCreatedAt);

		assertThat(overlayTime).isEqualTo("12:30");
	}

	@Test
	void handlesUtcMidnightAsKstNineAm() {
		LocalDateTime utcCreatedAt = LocalDateTime.ofInstant(
				java.time.Instant.parse("2026-01-01T00:00:00Z"),
				ZoneOffset.UTC
		);

		String overlayTime = OverlayTimeFormatter.formatKstHhMm(utcCreatedAt);

		assertThat(overlayTime).isEqualTo("09:00");
	}
}
