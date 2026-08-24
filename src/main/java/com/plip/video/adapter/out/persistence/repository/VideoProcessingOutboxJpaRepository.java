package com.plip.video.adapter.out.persistence.repository;

import com.plip.video.adapter.out.persistence.entity.VideoProcessingOutboxEntity;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VideoProcessingOutboxJpaRepository extends JpaRepository<VideoProcessingOutboxEntity, Long> {

	@Query(
			value = """
					SELECT *
					FROM video_processing_outbox
					WHERE status = 'PENDING'
					  AND next_attempt_at <= :now
					ORDER BY id ASC
					LIMIT :limit
					FOR UPDATE SKIP LOCKED
					""",
			nativeQuery = true
	)
	List<VideoProcessingOutboxEntity> findPendingForUpdate(
			@Param("now") LocalDateTime now,
			@Param("limit") int limit
	);

}
