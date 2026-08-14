package com.plip.video.adapter.out.persistence.repository;

import com.plip.video.adapter.out.persistence.entity.VideoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface VideoJpaRepository extends JpaRepository<VideoEntity, Long> {

	Optional<VideoEntity> findByVideoUuidAndDeletedAtIsNull(UUID videoUuid);
}
