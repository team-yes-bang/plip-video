package com.plip.video.application.port.out;

import java.util.UUID;

public interface ThumbnailLambdaPort {

	void invokeThumbnailGeneration(UUID videoUuid, String rawS3Key);
}
