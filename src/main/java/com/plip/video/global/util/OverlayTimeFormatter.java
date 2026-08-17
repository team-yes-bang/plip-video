package com.plip.video.global.util;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public final class OverlayTimeFormatter {

	private static final ZoneId KST = ZoneId.of("Asia/Seoul");
	private static final DateTimeFormatter HH_MM = DateTimeFormatter.ofPattern("HH:mm");

	private OverlayTimeFormatter() {
	}

	public static String formatKstHhMm(LocalDateTime createdAt) {
		if (createdAt == null) {
			throw new IllegalArgumentException("createdAt is required");
		}
		return createdAt.atZone(ZoneId.of("UTC"))
				.withZoneSameInstant(KST)
				.format(HH_MM);
	}
}
