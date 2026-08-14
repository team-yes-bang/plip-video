package com.plip.video.application.port.out;

import java.util.UUID;

public interface VideoProcessingQueuePort {

	void enqueueForDownloadProcessing(UUID videoUuid, String rawVideoPath);
}
