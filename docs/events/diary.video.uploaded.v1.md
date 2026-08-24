# diary.video.uploaded v1 (video-service Producer)

다이어리 **테마**에 영상을 바인딩할 때 video-service가 발행한다.

Consumer 상세: plip-diary `docs/events/diary.video.uploaded.v1.md`

## Topic

| 항목 | 값 |
| --- | --- |
| Topic | `diary.video.uploaded` |
| Producer | **video-service** |
| Consumer | diary-service |
| Message Key | `videoUuid` |
| Value format | JSON |

## 발행 트리거

- `POST /api/videos/{videoUuid}/destination` — request body `kind: "DIARY"`
- **선행 조건:** 동일 `videoUuid`에 대해 `POST .../complete`가 이미 성공
- `complete` 직후 자동 발행 **하지 않음**

## Payload (video-service가 produce하는 필드)

```json
{
  "themeUuid": "01912345-6789-7abc-def0-123456789abc",
  "videoUuid": "01912345-6789-7abc-def0-123456789abd",
  "userUuid": "01912345-6789-7abc-def0-123456789abe",
  "caption": "캡션",
  "thumbnailUrl": null,
  "occurredAt": "2026-08-12T11:00:00"
}
```

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| themeUuid | UUID | Y | `diary_themes.theme_uuid` (**numeric themeId 아님**) |
| videoUuid | UUID | Y | 영상 UUID |
| userUuid | UUID | Y | 업로더 — diary consumer 소유권 검증 |
| caption | string | N | Mongo projection upsert |
| thumbnailUrl | string | N | complete 직후 null 허용 |
| occurredAt | datetime (ISO-8601) | Y | produce 시각 |

## Producer 동작 (video-service)

1. destination REST에서 영상 존재 + 소유자 검증
2. `themeUuid` UUID 형식 검증
3. DB 트랜잭션 커밋 후 Kafka produce (`KafkaVideoDestinationEventAdapter`, `app.kafka.enabled=true`)
4. 테마 존재·당일 20건 한도는 **diary-service consumer** 책임

## 설정

```yaml
app.kafka.topics.diary-video-uploaded: diary.video.uploaded  # env: APP_KAFKA_TOPICS_DIARY_VIDEO_UPLOADED
```

## 버전 이력

| 버전 | 변경 |
| --- | --- |
| v1 | destination REST 트리거, themeUuid(UUID) 명시 |
