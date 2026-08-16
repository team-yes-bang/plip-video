package com.plip.video.adapter.in.web.controller;

import com.plip.video.adapter.in.web.dto.VideoRegisterResponse;
import com.plip.video.application.port.in.VideoUseCase;
import com.plip.video.application.port.in.dto.VideoRegisterCommand;
import com.plip.video.application.port.in.dto.VideoRegisterResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Tag(name = "Video", description = "5초 영상 촬영/업로드 API (스켈레톤 — CRUD 단계에서 재검토)")
@RestController
@RequestMapping("/api/videos")
@RequiredArgsConstructor
public class VideoController {

	private final VideoUseCase videoUseCase;

	@Operation(summary = "영상 업로드", description = """
			영상 파일 업로드 후 DB·S3에 저장하고 videoUuid 를 반환합니다.
			Agit/Diary 등록은 각 서비스가 반환된 videoUuid 로 처리합니다.
			- 캡션은 선택 (없으면 null)
			- HH:mm 오버레이는 created_at(업로드 시각) 기준
			""")
	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@ResponseStatus(HttpStatus.CREATED)
	public VideoRegisterResponse register(
			@Parameter(description = "업로더 UUID") @RequestParam UUID userUuid,
			@Parameter(description = "영상 파일 (최대 5초)") @RequestParam MultipartFile videoFile,
			@Parameter(description = "캡션 (선택)") @RequestParam(required = false) String caption
	) {
		VideoRegisterResult result = videoUseCase.register(new VideoRegisterCommand(
				userUuid,
				videoFile,
				caption
		));

		return new VideoRegisterResponse(
				result.videoUuid(),
				result.caption(),
				result.createdAt(),
				result.thumbnailUrl()
		);
	}

	@Operation(summary = "다운로드용 영상 가공 요청", description = "SQS → Lambda(ffmpeg) 파이프라인을 트리거합니다. (스켈레톤)")
	@PostMapping("/{videoUuid}/download-processing")
	@ResponseStatus(HttpStatus.ACCEPTED)
	public void requestDownloadProcessing(@PathVariable UUID videoUuid) {
		videoUseCase.requestDownloadProcessing(videoUuid);
	}
}
