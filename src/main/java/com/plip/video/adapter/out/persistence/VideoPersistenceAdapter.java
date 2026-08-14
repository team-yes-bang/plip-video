package com.plip.video.adapter.out.persistence;

import com.plip.video.adapter.out.persistence.entity.VideoEntity;
import com.plip.video.adapter.out.persistence.mapper.VideoEntityMapper;
import com.plip.video.adapter.out.persistence.repository.VideoJpaRepository;
import com.plip.video.application.port.out.VideoPersistencePort;
import com.plip.video.domain.model.Video;
import com.plip.video.domain.model.enums.VideoProcessingStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class VideoPersistenceAdapter implements VideoPersistencePort {

	private final VideoJpaRepository videoJpaRepository;
	private final VideoEntityMapper videoEntityMapper;

	@Override
	public Video save(Video video) {
		VideoEntity entity = videoEntityMapper.toEntity(video);
		VideoEntity saved = videoJpaRepository.save(entity);
		return videoEntityMapper.toDomain(saved);
	}

	@Override
	public Optional<Video> findByVideoUuid(UUID videoUuid) {
		return videoJpaRepository.findByVideoUuidAndDeletedAtIsNull(videoUuid)
				.map(videoEntityMapper::toDomain);
	}

	@Override
	public void updateProcessingStatus(UUID videoUuid, VideoProcessingStatus status, String processedFilePath) {
		videoJpaRepository.findByVideoUuidAndDeletedAtIsNull(videoUuid).ifPresent(entity -> {
			switch (status) {
				case PROCESSING -> entity.markProcessing();
				case FAILED -> entity.markFailed();
				case READY -> entity.markReady(processedFilePath);
				default -> { /* PENDING — no entity transition */ }
			}
		});
	}
}
