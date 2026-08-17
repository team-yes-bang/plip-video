package com.plip.video.adapter.in.web.controller;

import com.plip.video.adapter.in.web.dto.VideoCompleteRequest;
import com.plip.video.adapter.in.web.dto.VideoCompleteResponse;
import com.plip.video.adapter.in.web.dto.VideoDetailResponse;
import com.plip.video.adapter.in.web.dto.VideoRegisterResponse;
import com.plip.video.adapter.in.web.dto.VideoUploadUrlResponse;
import com.plip.video.application.port.in.VideoUseCase;
import com.plip.video.application.port.in.dto.VideoCompleteCommand;
import com.plip.video.application.port.in.dto.VideoCompleteResult;
import com.plip.video.application.port.in.dto.VideoDetailResult;
import com.plip.video.application.port.in.dto.VideoRegisterCommand;
import com.plip.video.application.port.in.dto.VideoRegisterResult;
import com.plip.video.application.port.in.dto.VideoUploadUrlCommand;
import com.plip.video.application.port.in.dto.VideoUploadUrlResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Tag(name = "Video", description = "5초 영상 Presigned 업로드·조회 API")
@RestController
@RequestMapping("/api/videos")
@RequiredArgsConstructor
public class VideoController {

	private final VideoUseCase videoUseCase;

	@Operation(summary = "Presigned upload URL 발급", description = """
			영상 UUID와 S3 Presigned PUT URL을 발급합니다.
			- DB INSERT 없음 (complete에서 저장)
			- rawS3Key = videos/raw/{videoUuid}.mp4
			""")
	@PostMapping("/upload-url")
	@ResponseStatus(HttpStatus.CREATED)
	public VideoUploadUrlResponse issueUploadUrl(
			@Parameter(description = "업로더 UUID") @RequestParam UUID userUuid,
			@Parameter(description = "Content-Type (선택, 기본 video/mp4)") @RequestParam(required = false) String contentType
	) {
		VideoUploadUrlResult result = videoUseCase.issueUploadUrl(new VideoUploadUrlCommand(userUuid, contentType));
		return new VideoUploadUrlResponse(
				result.videoUuid(),
				result.rawS3Key(),
				result.uploadUrl(),
				result.expiresAt()
		);
	}

	@Operation(summary = "영상 업로드 complete", description = """
			S3 업로드 완료 후 메타데이터를 DB에 저장합니다.
			- HeadObject로 raw 파일 존재·용량 검증
			- thumbnail_image_path = null (Lambda callback 대기)
			- overlayTime = created_at KST HH:mm
			""")
	@PostMapping("/{videoUuid}/complete")
	@ResponseStatus(HttpStatus.CREATED)
	public VideoCompleteResponse complete(
			@Parameter(description = "upload-url에서 발급된 video UUID") @PathVariable UUID videoUuid,
			@Parameter(description = "업로더 UUID") @RequestParam UUID userUuid,
			@RequestBody(required = false) VideoCompleteRequest request
	) {
		String caption = request != null ? request.caption() : null;
		VideoCompleteResult result = videoUseCase.complete(new VideoCompleteCommand(videoUuid, userUuid, caption));
		return new VideoCompleteResponse(
				result.videoUuid(),
				result.caption(),
				result.createdAt(),
				result.overlayTime()
		);
	}

	@Operation(summary = "영상 상세 조회", description = """
			피드 재생용 영상 메타데이터와 raw Presigned GET URL을 반환합니다.
			- rawPlaybackUrl: raw bucket Presigned GET (TTL 3h)
			- thumbnailUrl: Lambda callback 전 null
			- overlayTime: created_at KST HH:mm (프론트 UI 오버레이)
			- downloadReady: processed_path 존재 여부
			""")
	@GetMapping("/{videoUuid}")
	public VideoDetailResponse getVideo(
			@Parameter(description = "영상 UUID") @PathVariable UUID videoUuid
	) {
		VideoDetailResult result = videoUseCase.getVideo(videoUuid);
		return new VideoDetailResponse(
				result.videoUuid(),
				result.userUuid(),
				result.caption(),
				result.createdAt(),
				result.rawPlaybackUrl(),
				result.thumbnailUrl(),
				result.overlayTime(),
				result.downloadReady()
		);
	}

	@Operation(summary = "[레거시] multipart 영상 업로드", description = "PR-6에서 제거 예정. upload-url + complete 사용 권장.")
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

	@Operation(summary = "[레거시] 다운로드용 영상 가공 요청", description = "PR-6에서 제거 예정. complete 시 자동 enqueue.")
	@PostMapping("/{videoUuid}/download-processing")
	@ResponseStatus(HttpStatus.ACCEPTED)
	public void requestDownloadProcessing(@PathVariable UUID videoUuid) {
		videoUseCase.requestDownloadProcessing(videoUuid);
	}
}
