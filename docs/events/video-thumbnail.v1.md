# video-thumbnail.v1

썸네일 추출 비동기 작업 (plip-video → Lambda async invoke → S3(A) → REST callback).

## Flow

```text
plip-video (POST /complete, DB row 생성 후)
  → Lambda InvokeFunction (Event, async)
  → Thumbnail Lambda (FFmpeg 첫 프레임)
  → S3(A) images/thumbnails/{videoUuid}.jpg
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

## Lambda responsibilities (인프ra/별도 repo)

1. S3(A)에서 raw 영상 다운로드
2. FFmpeg 첫 프레임 추출 → `images/thumbnails/{videoUuid}.jpg` 업로드
3. plip-video internal REST callback으로 `thumbnail_image_path` 갱신

## DB fields updated on success

| column | value |
| --- | --- |
| `thumbnail_image_path` | `images/thumbnails/{videoUuid}.jpg` (complete 시 NULL) |
