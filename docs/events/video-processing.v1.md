# video-processing.v1

영상 다운로드용 가공 파이프라인 이벤트 (SQS → AWS Lambda ffmpeg → S3).

## Flow

```text
video service
  → S3 (raw video + thumbnail image, relative path)
  → SQS (video-processing queue)
  → Lambda (ffmpeg transcode/watermark if needed)
  → S3 (processed video bucket/prefix)
  → video service DB update (processed_path)
```

## Message body (v1)

```json
{
  "videoUuid": "0195xxxx-xxxx-7xxx-xxxx-xxxxxxxxxxxx",
  "rawVideoPath": "videos/raw/{videoUuid}.mp4"
}
```

## Lambda responsibilities (인프라/별도 repo)

1. SQS 메시지 수신
2. S3 raw 경로에서 원본 다운로드
3. ffmpeg로 다운로드용 포맷/품질 가공 (필요 시 워터마크·메타데이터)
4. `videos/processed/{videoUuid}.mp4` 등 **상대 경로**로 S3 업로드
5. video service callback API 또는 DB 직접 갱신 **(팀 협의 필요 — 아래 FAQ 참고)**

## DB fields updated on success

| column | value |
| --- | --- |
| `processed_path` | relative S3 path (다운로드용 가공본) |

## Related requirements mapping

| # | requirement | handled by |
| --- | --- | --- |
| 7 | 첫 프레임 S3 img + 영상 S3 저장 | video service upload |
| 8 | S3 → SQS → Lambda → S3 가공 | this pipeline |

## Open questions

- Lambda 완료 후 video DB 갱신 방식: REST callback vs Kafka event vs 직접 DB (MSA 원칙상 callback/event 권장)
- ffmpeg 워터마크(캡션·HH:mm)를 서버에서 할지, 클라이언트 첨부 시 이미 합성할지 — HH:mm 기준 시각은 `created_at`(업로드 시각)
