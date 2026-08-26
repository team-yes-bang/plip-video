package com.plip.video.global.util;

import com.github.f4b6a3.uuid.UuidCreator;

import java.util.UUID;

public final class VideoUuidGenerator {

	private VideoUuidGenerator() {
	}

	public static UUID generate() {
		return UuidCreator.getTimeOrderedEpoch();
	}
}
