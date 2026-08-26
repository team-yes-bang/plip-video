package com.plip.video.adapter.in.web.controller;

import com.plip.video.adapter.in.web.dto.InternalUpdateProcessedRequest;
import com.plip.video.adapter.in.web.dto.InternalUpdateThumbnailRequest;
import com.plip.video.adapter.in.web.dto.InternalVideoOwnershipResponse;
import com.plip.video.application.port.in.VideoUseCase;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Hidden
@Tag(name = "Internal Video", description = "Lambda callback 전용 (Gateway 외부 라우팅 제외, X-Internal-Api-Key 필수)")
@RestController
@RequestMapping("/internal/videos")
@RequiredArgsConstructor
public class InternalVideoController {

	private final VideoUseCase videoUseCase;

	@Operation(summary = "영상 소유자 조회", description = "topic/diary attach 시 소유 검증용 내부 API")
	@GetMapping("/{videoUuid}")
	public InternalVideoOwnershipResponse getOwnership(
			@Parameter(description = "영상 UUID") @PathVariable UUID videoUuid
	) {
		var result = videoUseCase.getOwnership(videoUuid);
		return new InternalVideoOwnershipResponse(result.videoUuid(), result.userUuid());
	}

	@Operation(summary = "썸네일 경로 갱신", description = "Thumbnail Lambda 완료 후 thumbnail_image_path 를 갱신합니다.")
	@PatchMapping("/{videoUuid}/thumbnail")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void updateThumbnail(
			@Parameter(description = "영상 UUID") @PathVariable UUID videoUuid,
			@Valid @RequestBody InternalUpdateThumbnailRequest request
	) {
		videoUseCase.updateThumbnail(videoUuid, request.thumbnailS3Key());
	}

	@Operation(summary = "가공본 경로 갱신", description = "FFmpeg Lambda 완료 후 processed_path 를 갱신합니다.")
	@PatchMapping("/{videoUuid}/processed")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void updateProcessed(
			@Parameter(description = "영상 UUID") @PathVariable UUID videoUuid,
			@Valid @RequestBody InternalUpdateProcessedRequest request
	) {
		videoUseCase.updateProcessed(videoUuid, request.processedS3Key(), request.durationSeconds());
	}
}
