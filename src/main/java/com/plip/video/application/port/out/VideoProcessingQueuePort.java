package com.plip.video.application.port.out;

import java.util.UUID;

public interface VideoProcessingQueuePort {

	void enqueueVideoProcessing(UUID videoUuid, String rawS3Key, String caption, String overlayTime);
}
