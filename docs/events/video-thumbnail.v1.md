# video-thumbnail.v1

썸네일 추출 비동기 작업 (plip-video → Lambda async invoke → S3(processed) → REST callback).

## Flow

```text
plip-video (POST /complete, DB row 생성 후)
  → Lambda InvokeFunction (Event, async)
  → Thumbnail Lambda (FFmpeg 첫 프레임)
  → S3(processed) thumbnail/{videoUuid}.jpg
  → plip-video REST callback (thumbnail_image_path 갱신)
```

> S3 Event 트리거 사용하지 않음 — Presigned PUT이 complete보다 먼저 일어날 수 있음.

## Invoke payload (v1)

```json
{
  "videoUuid": "0195xxxx-xxxx-7xxx-xxxx-xxxxxxxxxxxx",
  "rawS3Key": "videos/raw/{videoUuid}.mp4"
}
```

## Lambda responsibilities

1. S3(raw)에서 raw 영상 다운로드
2. FFmpeg 첫 프레임 추출 → `thumbnail/{videoUuid}.jpg` 를 S3(processed)에 업로드
3. plip-video internal REST callback으로 `thumbnail_image_path` 갱신

## DB fields updated on success

| column | value |
| --- | --- |
| `thumbnail_image_path` | `thumbnail/{videoUuid}.jpg` (complete 시 NULL) |

## Deploy

- Lambda: `terraform/lambda/thumbnail/deploy.sh`
- K8s: `fo-ingress` `/internal/videos` → plip-video, NetworkPolicy Traefik 허용
- plip-video: `plip.aws.s3.thumbnail-prefix=thumbnail/`
