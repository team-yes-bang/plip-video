# POST /api/v1/videos/{videoUuid}/destination v1

업로드 complete 이후, 영상을 **토픽** 또는 **다이어리 테마**에 비동기로 붙이기 위한 REST 계약.

OpenAPI: `docs/openapi.yaml` — `publishDestination`

## Flow

```text
Front: POST complete(caption)
Front: POST /destination { kind, topicUuid | themeUuid }
video-service: 202 Accepted (Phase 1+) → Kafka produce (Phase 2, `app.kafka.enabled=true`)
topic/diary consumer: DB attach (수 초 내)
```

- `complete` body에 destination **포함하지 않음**
- 프론트는 Kafka에 직접 produce하지 않음
- `video.destination.requested` 토픽 **없음**

## Request

```
POST /api/v1/videos/{videoUuid}/destination
Content-Type: application/json
Authorization: Bearer {JWT}   # Phase 1b — Gateway userUuid (query userUuid 사용 안 함)
```

### Path

| name | type | 설명 |
| --- | --- | --- |
| videoUuid | UUID | complete된 영상 UUID |

### Body

```json
{
  "kind": "TOPIC",
  "topicUuid": "0190abcd-1111-7abc-def0-123456789abc",
  "agitUuid": "018f3f6e-8e2a-7b3c-9d4e-5f6a7b8c9d0e",
  "caption": "선택 캡션"
}
```

```json
{
  "kind": "DIARY",
  "themeUuid": "01912345-6789-7abc-def0-123456789abc",
  "caption": "선택 캡션"
}
```

| field | type | TOPIC | DIARY | 설명 |
| --- | --- | --- | --- | --- |
| kind | `"TOPIC"` \| `"DIARY"` | Y | Y | 부착 대상 종류 |
| topicUuid | UUID | **Y** | — | 아지트 토픽 UUID |
| themeUuid | UUID | — | **Y** | 다이어리 테마 UUID (`themeId` 숫자 아님) |
| agitUuid | UUID | N | — | 프론트 컨텍스트·로깅용. consumer 검증은 topicUuid 기준 |
| caption | string | N | N | 없으면 DB `video.caption` 사용 |

캡처 MVP: **TOPIC 또는 DIARY 단일 선택** (`BOTH` 없음). dual 필요 시 이벤트 2건 produce (Phase 2+).

## Response

### 202 Accepted (Phase 1+)

```json
{
  "videoUuid": "01912345-6789-7abc-def0-123456789abd",
  "status": "PUBLISHED"
}
```

Kafka가 꺼져 있어도(`app.kafka.enabled=false`) **202**를 반환한다. Phase 2에서 실제 produce가 연결된다.

### 인증 (Phase 1)

`userUuid`는 다음 중 하나로 전달:

- Query: `?userUuid=...` (complete API와 동일, 로컬 curl)
- Header: `X-User-Uuid` 또는 `X-User-UUID` (프론트 Server Action / Gateway)

### Errors

| HTTP | 조건 |
| --- | --- |
| 400 | kind/UUID 필드 누락·형식 오류 |
| 401 | 인증 없음 (Phase 1b) |
| 403 | video 소유자 ≠ 요청 user |
| 404 | videoUuid 미존재 (complete 안 됨) |

## Kafka mapping (Phase 2)

| kind | Topic | 필수 payload |
| --- | --- | --- |
| TOPIC | `video.uploaded` | topicUuid, videoUuid, userUuid, occurredAt |
| DIARY | `diary.video.uploaded` | themeUuid, videoUuid, userUuid, occurredAt |

## 버전 이력

| 버전 | 변경 |
| --- | --- |
| v1 | 최초 — kind + topicUuid/themeUuid, complete 분리 |
