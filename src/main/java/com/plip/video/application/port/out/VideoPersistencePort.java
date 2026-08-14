package com.plip.video.application.port.out;

import com.plip.video.domain.model.Video;
import com.plip.video.domain.model.enums.VideoProcessingStatus;

import java.util.Optional;
import java.util.UUID;

public interface VideoPersistencePort {

	Video save(Video video);

	Optional<Video> findByVideoUuid(UUID videoUuid);

	void updateProcessingStatus(UUID videoUuid, VideoProcessingStatus status, String processedFilePath);
}
