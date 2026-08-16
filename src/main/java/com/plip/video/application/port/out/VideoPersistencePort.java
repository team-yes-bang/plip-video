package com.plip.video.application.port.out;

import com.plip.video.domain.model.Video;

import java.util.Optional;
import java.util.UUID;

public interface VideoPersistencePort {

	Video save(Video video);

	Optional<Video> findByVideoUuid(UUID videoUuid);
}
