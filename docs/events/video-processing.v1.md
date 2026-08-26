# video-processing.v1

영상 다운로드용 가공 파이프라인 이벤트 (SQS → AWS Lambda ffmpeg → S3).

## Flow

```text
plip-video (POST /complete)
  → outbox → SQS (video-processing queue)
  → Lambda (ffprobe duration check → ffmpeg burn-in)
  → S3 (processed bucket)
  → plip-video REST callback (processed_path + durationSeconds)
```

## Message body (v1)

```json
{
  "videoUuid": "0195xxxx-xxxx-7xxx-xxxx-xxxxxxxxxxxx",
  "rawS3Key": "videos/raw/{videoUuid}.mp4",
  "caption": "optional",
  "overlayTime": "20:07",
  "maxDurationSeconds": 5
}
```

| field | description |
| --- | --- |
| `rawS3Key` | S3(A) raw **상대 경로** (`file_path`와 동일) |
| `caption` | 선택. 없으면 생략 또는 null |
| `overlayTime` | `created_at` KST `HH:mm` — burn-in drawtext용 |
| `maxDurationSeconds` | plip-video `plip.video.max-duration-seconds` (기본 5). **초과 시 Lambda는 가공/콜백 금지** |

## Lambda responsibilities

1. SQS 메시지 수신
2. S3 raw 다운로드
3. **ffprobe로 duration 측정** → `duration > maxDurationSeconds` 이면 **실패(재시도/콜백 없음)**
4. ffmpeg burn-in (overlayTime, caption)
5. `videos/processed/{videoUuid}.mp4` 업로드
6. Callback:

```http
PATCH /internal/videos/{videoUuid}/processed
X-Internal-Api-Key: ...
Content-Type: application/json

{
  "processedS3Key": "videos/processed/{videoUuid}.mp4",
  "durationSeconds": 4
}
```

| field | required | description |
| --- | --- | --- |
| `processedS3Key` | Y | `videos/processed/` prefix 필수 |
| `durationSeconds` | Y | 초 단위(ceil). **video 서비스가 max 재검증** — 초과 시 4xx |

## DB fields updated on success

| column | value |
| --- | --- |
| `processed_path` | relative S3 path (다운로드용 가공본) |

## Related requirements mapping

| # | requirement | handled by |
| --- | --- | --- |
| 7 | 첫 프레임 S3 img + 영상 S3 저장 | thumbnail pipeline (별도) |
| 8 | S3 → SQS → Lambda → S3 가공 | this pipeline |
| duration cap | max N초 | SQS `maxDurationSeconds` + Lambda ffprobe + callback 재검증 |

## Resolved decisions

- SQS publish: **plip-video `complete` 후 outbox** (S3 Event 아님)
- DB 갱신: **REST internal callback** (Kafka/직접 DB 아님)
- HH:mm 기준: **`created_at`(업로드 시각)**, `recordedAt` 없음
- Duration: Lambda가 1차 차단, video callback이 2차 차단
