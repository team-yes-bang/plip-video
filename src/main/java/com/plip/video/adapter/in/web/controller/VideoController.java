package com.plip.video.adapter.in.web.controller;

import com.plip.video.adapter.in.web.AuthenticatedActor;
import com.plip.video.adapter.in.web.dto.VideoCompleteRequest;
import com.plip.video.adapter.in.web.dto.VideoCompleteResponse;
import com.plip.video.adapter.in.web.dto.VideoDestinationRequest;
import com.plip.video.adapter.in.web.dto.VideoDestinationResponse;
import com.plip.video.adapter.in.web.dto.VideoDetailResponse;
import com.plip.video.adapter.in.web.dto.VideoDownloadUrlProcessingResponse;
import com.plip.video.adapter.in.web.dto.VideoDownloadUrlResponse;
import com.plip.video.adapter.in.web.dto.VideoUploadUrlResponse;
import com.plip.video.application.port.in.VideoUseCase;
import com.plip.video.application.port.in.dto.VideoCompleteCommand;
import com.plip.video.application.port.in.dto.VideoCompleteResult;
import com.plip.video.application.port.in.dto.VideoDestinationCommand;
import com.plip.video.application.port.in.dto.VideoDestinationKind;
import com.plip.video.application.port.in.dto.VideoDestinationResult;
import com.plip.video.application.port.in.dto.VideoDetailResult;
import com.plip.video.application.port.in.dto.VideoDownloadUrlProcessing;
import com.plip.video.application.port.in.dto.VideoDownloadUrlReady;
import com.plip.video.application.port.in.dto.VideoDownloadUrlResult;
import com.plip.video.application.port.in.dto.VideoUploadUrlCommand;
import com.plip.video.application.port.in.dto.VideoUploadUrlResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

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
			- contentLengthBytes 필수: 서명에 Content-Length 포함 → 초과 업로드 불가
			- Gateway X-User-UUID 필수
			""")
	@PostMapping("/upload-url")
	@ResponseStatus(HttpStatus.CREATED)
	public VideoUploadUrlResponse issueUploadUrl(
			@Parameter(description = "Content-Type (선택, 기본 video/mp4)") @RequestParam(required = false) String contentType,
			@Parameter(description = "업로드할 바이트 수 (필수, maxFileSizeBytes 이하)") @RequestParam long contentLengthBytes
	) {
		UUID actorUuid = AuthenticatedActor.requireUserUuid();
		VideoUploadUrlResult result = videoUseCase.issueUploadUrl(
				new VideoUploadUrlCommand(actorUuid, contentType, contentLengthBytes)
		);
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
			- 업로더는 Gateway X-User-UUID (헤더 위조 방지)
			""")
	@PostMapping("/{videoUuid}/complete")
	public ResponseEntity<VideoCompleteResponse> complete(
			@Parameter(description = "upload-url에서 발급된 video UUID") @PathVariable UUID videoUuid,
			@RequestBody(required = false) VideoCompleteRequest request
	) {
		UUID actorUuid = AuthenticatedActor.requireUserUuid();
		String caption = request != null ? request.caption() : null;
		VideoCompleteResult result = videoUseCase.complete(new VideoCompleteCommand(videoUuid, actorUuid, caption));
		VideoCompleteResponse body = new VideoCompleteResponse(
				result.videoUuid(),
				result.caption(),
				result.createdAt(),
				result.overlayTime()
		);
		HttpStatus status = result.newlyCreated() ? HttpStatus.CREATED : HttpStatus.OK;
		return ResponseEntity.status(status).body(body);
	}

	@Operation(summary = "영상 상세 조회", description = """
			피드 재생용 영상 메타데이터와 raw Presigned GET URL을 반환합니다.
			- rawPlaybackUrl: raw bucket Presigned GET (TTL 3h)
			- thumbnailUrl: Lambda callback 전 null
			- overlayTime: created_at KST HH:mm (프론트 UI 오버레이)
			- downloadReady: processed_path 존재 여부
			- Gateway X-User-UUID 필수 (익명 Presigned URL 발급 차단)
			""")
	@GetMapping("/{videoUuid}")
	public VideoDetailResponse getVideo(
			@Parameter(description = "영상 UUID") @PathVariable UUID videoUuid
	) {
		UUID actorUuid = AuthenticatedActor.requireUserUuid();
		VideoDetailResult result = videoUseCase.getVideo(videoUuid, actorUuid);
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

	@Operation(summary = "다운로드 URL 조회", description = """
			가공본(processed) CloudFront URL을 반환합니다.
			- processed_path 없음 → 202 Accepted + Retry-After + PROCESSING body
			- processed_path 있음 → 200 OK + downloadUrl
			- Gateway X-User-UUID 필수
			""")
	@GetMapping("/{videoUuid}/download-url")
	public ResponseEntity<?> getDownloadUrl(
			@Parameter(description = "영상 UUID") @PathVariable UUID videoUuid
	) {
		UUID actorUuid = AuthenticatedActor.requireUserUuid();
		VideoDownloadUrlResult result = videoUseCase.getDownloadUrl(videoUuid, actorUuid);

		if (result instanceof VideoDownloadUrlProcessing processing) {
			return ResponseEntity.status(HttpStatus.ACCEPTED)
					.header(HttpHeaders.RETRY_AFTER, String.valueOf(processing.retryAfterSeconds()))
					.body(new VideoDownloadUrlProcessingResponse(
							"PROCESSING",
							processing.videoUuid(),
							processing.retryAfterSeconds(),
							processing.message()
					));
		}

		VideoDownloadUrlReady ready = (VideoDownloadUrlReady) result;
		return ResponseEntity.ok(new VideoDownloadUrlResponse(ready.videoUuid(), ready.downloadUrl()));
	}

	@Operation(summary = "영상 destination publish", description = """
			complete 이후 토픽/다이어리 테마에 영상을 비동기 부착 요청합니다.
			- kind=TOPIC → Kafka `video.uploaded`
			- kind=DIARY → Kafka `diary.video.uploaded`
			- actor는 Gateway X-User-UUID (video 소유자와 일치해야 함)
			- 상세: docs/api/video-destination.v1.md
			""")
	@ApiResponses({
			@ApiResponse(responseCode = "202", description = "Kafka produce 접수"),
			@ApiResponse(responseCode = "400", description = "kind/UUID 필드 오류"),
			@ApiResponse(responseCode = "403", description = "video 소유자 불일치"),
			@ApiResponse(responseCode = "404", description = "video 미존재 (complete 안 됨)")
	})
	@PostMapping("/{videoUuid}/destination")
	public ResponseEntity<VideoDestinationResponse> publishDestination(
			@Parameter(description = "complete된 영상 UUID") @PathVariable UUID videoUuid,
			@RequestBody VideoDestinationRequest request
	) {
		UUID actorUuid = AuthenticatedActor.requireUserUuid();
		VideoDestinationResult result = videoUseCase.publishDestination(new VideoDestinationCommand(
				videoUuid,
				actorUuid,
				mapKind(request.kind()),
				request.topicUuid(),
				request.themeUuid(),
				request.agitUuid(),
				request.caption()
		));
		return ResponseEntity.status(HttpStatus.ACCEPTED)
				.body(new VideoDestinationResponse(result.videoUuid(), result.status()));
	}

	private VideoDestinationKind mapKind(com.plip.video.adapter.in.web.dto.VideoDestinationKind kind) {
		if (kind == null) {
			throw new IllegalArgumentException("kind is required");
		}
		return VideoDestinationKind.valueOf(kind.name());
	}
}
