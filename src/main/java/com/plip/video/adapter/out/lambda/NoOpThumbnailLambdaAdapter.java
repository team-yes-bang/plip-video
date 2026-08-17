package com.plip.video.adapter.out.lambda;

import com.plip.video.application.port.out.ThumbnailLambdaPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "plip.aws", name = "enabled", havingValue = "false", matchIfMissing = true)
public class NoOpThumbnailLambdaAdapter implements ThumbnailLambdaPort {

	@Override
	public void invokeThumbnailGeneration(UUID videoUuid, String rawS3Key) {
		log.warn("AWS disabled — stub thumbnail lambda invoke: videoUuid={}, rawS3Key={}", videoUuid, rawS3Key);
	}
}
