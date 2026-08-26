package com.plip.video.application.port.out;

import java.util.UUID;

public interface VideoProcessingOutboxPort {

	void enqueueProcessingJobs(
			UUID videoUuid,
			String rawS3Key,
			String caption,
			String overlayTime,
			int maxDurationSeconds
	);

}
