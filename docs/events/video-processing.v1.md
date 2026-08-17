# video-processing.v1

영상 다운로드용 가공 파이프라인 이벤트 (SQS → AWS Lambda ffmpeg → S3).

## Flow

```text
plip-video (POST /complete)
  → SQS (video-processing queue)
  → Lambda (ffmpeg burn-in: overlayTime, caption)
  → S3 (processed bucket)
  → plip-video REST callback (processed_path 갱신)
```

## Message body (v1)

```json
{
  "videoUuid": "0195xxxx-xxxx-7xxx-xxxx-xxxxxxxxxxxx",
  "rawS3Key": "videos/raw/{videoUuid}.mp4",
  "caption": "optional",
  "overlayTime": "20:07"
}
```

| field | description |
| --- | --- |
| `rawS3Key` | S3(A) raw **상대 경로** (`file_path`와 동일) |
| `caption` | 선택. 없으면 생략 또는 null |
| `overlayTime` | `created_at` KST `HH:mm` — burn-in drawtext용 |

## Lambda responsibilities (인프ra/별도 repo)

1. SQS 메시지 수신
2. S3 raw 경로에서 원본 다운로드
3. ffmpeg로 burn-in 가공 (overlayTime, caption)
4. `videos/processed/{videoUuid}.mp4` **상대 경로**로 S3(B) 업로드
5. plip-video internal REST callback으로 `processed_path` 갱신

## DB fields updated on success

| column | value |
| --- | --- |
| `processed_path` | relative S3 path (다운로드용 가공본) |

## Related requirements mapping

| # | requirement | handled by |
| --- | --- | --- |
| 7 | 첫 프레임 S3 img + 영상 S3 저장 | thumbnail pipeline (별도) |
| 8 | S3 → SQS → Lambda → S3 가공 | this pipeline |

## Resolved decisions

- SQS publish: **plip-video `complete` 후** (S3 Event 아님)
- DB 갱신: **REST internal callback** (Kafka/직접 DB 아님)
- HH:mm 기준: **`created_at`(업로드 시각)**, `recordedAt` 없음
