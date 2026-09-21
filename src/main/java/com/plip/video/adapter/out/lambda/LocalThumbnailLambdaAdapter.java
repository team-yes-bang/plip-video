package com.plip.video.adapter.out.lambda;

import com.plip.video.application.port.out.ThumbnailLambdaPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "plip.storage", name = "type", havingValue = "local")
public class LocalThumbnailLambdaAdapter implements ThumbnailLambdaPort {

	@Override
	public void invokeThumbnailGeneration(UUID videoUuid, String rawS3Key) {
		log.info(
				"Local storage mode — auto thumbnail generation skipped: videoUuid={}, rawKey={}. "
						+ "Use thumbnail-upload-url before complete or accept null thumbnail.",
				videoUuid,
				rawS3Key
		);
	}
}
