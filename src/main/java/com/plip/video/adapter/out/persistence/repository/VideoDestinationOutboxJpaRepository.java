package com.plip.video.adapter.out.persistence.repository;

import com.plip.video.adapter.out.persistence.entity.VideoDestinationOutboxEntity;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VideoDestinationOutboxJpaRepository extends JpaRepository<VideoDestinationOutboxEntity, Long> {

	@Query(
			value = """
					SELECT *
					FROM video_destination_outbox
					WHERE status = 'PENDING'
					  AND next_attempt_at <= :now
					ORDER BY id ASC
					LIMIT :limit
					FOR UPDATE SKIP LOCKED
					""",
			nativeQuery = true
	)
	List<VideoDestinationOutboxEntity> findPendingForUpdate(
			@Param("now") LocalDateTime now,
			@Param("limit") int limit
	);

}
