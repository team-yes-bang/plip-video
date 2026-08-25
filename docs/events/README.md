# Event specifications

Kafka / EDA 스펙은 `docs/events/{event-name}.v1.md`에 둔다.

| 이벤트 | 이 서비스 역할 | Consumer | 발행 트리거 |
| --- | --- | --- | --- |
| [video.uploaded.v1](video.uploaded.v1.md) | **Producer** | topic-service | `POST /api/v1/videos/{uuid}/destination` (`kind=TOPIC`) |
| [diary.video.uploaded.v1](diary.video.uploaded.v1.md) | **Producer** | diary-service | `POST /api/v1/videos/{uuid}/destination` (`kind=DIARY`) |

Consumer 쪽 상세(멱등·한도·DB)는 각 서비스 docs를 따른다.

- topic 구독: plip-topic `docs/events/video.uploaded.v1.md`
- diary 구독: plip-diary `docs/events/diary.video.uploaded.v1.md`

> `video.destination.requested` 같은 통합 토픽은 **사용하지 않는다.** destination kind별로 위 두 토픽만 produce한다.
