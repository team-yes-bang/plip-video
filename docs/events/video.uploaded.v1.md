# video.uploaded v1 (video-service Producer)

아지트 **토픽**에 영상을 붙일 때 video-service가 발행한다.

Consumer 상세: plip-topic `docs/events/video.uploaded.v1.md`

## Topic

| 항목 | 값 |
| --- | --- |
| Topic | `video.uploaded` |
| Producer | **video-service** |
| Consumer | topic-service |
| Message Key | `videoUuid` |
| Value format | JSON (type header 없음) |

## 발행 트리거

- `POST /api/videos/{videoUuid}/destination` — request body `kind: "TOPIC"`
- **선행 조건:** 동일 `videoUuid`에 대해 `POST .../complete`가 이미 성공( DB row 존재 )
- `complete` 직후 자동 발행 **하지 않음** (destination REST와 분리)

## Payload (video-service가 produce하는 필드)

```json
{
  "topicUuid": "0190abcd-1111-7abc-def0-123456789abc",
  "videoUuid": "01912345-6789-7abc-def0-123456789abd",
  "userUuid": "01912345-6789-7abc-def0-123456789abe",
  "caption": "캡션",
  "occurredAt": "2026-08-12T11:00:00"
}
```

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| topicUuid | UUID | Y | 붙일 토픽 |
| videoUuid | UUID | Y | 영상 UUID |
| userUuid | UUID | Y | 업로더. topic consumer가 `tryAttach`에 전달 |
| caption | string | N | complete 시 저장값 또는 destination body |
| occurredAt | datetime (ISO-8601) | Y | produce 시각 |
| themeUuid | UUID | N | **TOPIC-only produce 시 생략** (topic consumer ignore) |
| thumbnailUrl | string | N | complete 직후 null 가능 |

## Producer 동작 (video-service)

1. destination REST에서 영상 존재 + `userUuid` 소유자 검증
2. `topicUuid` UUID 형식 검증
3. DB 트랜잭션 커밋 후 Kafka produce (`KafkaVideoDestinationEventAdapter`, `app.kafka.enabled=true`)
4. agit 멤버십·토픽 존재 검증은 **topic-service consumer** 책임

## 설정

```yaml
app.kafka.topics.video-uploaded: video.uploaded  # env: APP_KAFKA_TOPICS_VIDEO_UPLOADED
```

## 버전 이력

| 버전 | 변경 |
| --- | --- |
| v1 | destination REST 트리거, userUuid 필수 (topic pull 반영) |
