# Cross-service 요청 스펙 (video → topic / agit)

> **작성 목적:** video 담당 범위(`plip-video`, `plip-gateway`, k8s manifest)만으로 커버되지 않는 연동을  
> topic / agit 팀에 요청하기 위한 스펙입니다.  
> video는 topic·agit 코드를 소유하지 않으며, 아래 API가 없으면 **해당 기능만** 제한됩니다.

---

## 0. Video만으로 이미 커버되는 것 (요청 불필요)

| 기능 | 동작 |
| --- | --- |
| 업로드 / complete / destination 발행 | video 단독 |
| 업로더 본인 재생·다운로드(presigned GET) | `actorUuid == video.userUuid` 이면 topic 호출 없이 허용 |
| 소유권 조회 API 제공 | `GET /internal/videos/{videoUuid}` (topic/diary가 호출) |
| Gateway HMAC 검증 (video) | gateway가 서명, video가 검증 |
| Topic access API 부재/장애 | 비소유자 재생만 거부(403). 업로더 재생·업로드는 유지 |

**한계:** 같은 아지트 멤버가 **남의 영상**을 재생하는 흐름은 video DB만으로는 판단 불가  
(`topic_video` 링크 + agit membership은 각각 topic / agit 데이터).

---

## 1. [필수] Topic — 재생 권한 확인 Internal API

### 왜 필요한가

Video는 “이 영상이 어떤 토픽에 붙어 있는지”, “요청자가 그 아지트 멤버인지”를 모릅니다.  
비소유자 재생 시 video가 topic에 묻고, topic이 **링크 + membership**을 판정해야 합니다.

없으면: 업로더만 재생 가능. 멤버 간 공유 재생 불가.

### 무엇을

```http
HEAD /internal/v1/videos/{videoUuid}/access/{userUuid}
X-Internal-Api-Key: <INTERNAL_API_KEY>
```

| 응답 | 의미 |
| --- | --- |
| `204 No Content` | `userUuid`가 해당 `videoUuid`를 볼 수 있음 |
| `403 Forbidden` | 링크 없음 또는 비멤버 |
| `401` | API key 없거나 불일치 |

### 어떻게 (권장 판정)

인가(재생 허용)용 Fact는 **RDB + Redis**를 쓰고, Mongo TopicViewer는 **UI 피드**용으로 둔다.

| Fact | 진실 / 조회 | 비고 |
| --- | --- | --- |
| A. video → agit | Topic RDB `topic_video` → `topic.agit_uuid` | eventual consistency인 Mongo 스냅샷으로 인가하지 말 것 |
| B. user ∈ agit | Agit가 관리하는 Redis Set `agit:{agitUuid}:members` (`SISMEMBER`) | Cache miss 시 agit internal members API로 warm-up |

권장 순서:

1. `videoUuid`로 `topic_video`(미삭제) → 연결된 `topic.agit_uuid` 목록 조회 (**RDB**)  
2. 각 `agitUuid`에 대해 Redis `SISMEMBER` (없으면 agit `GET /internal/v1/agits/{agitUuid}/members` warm-up)  
3. 하나라도 멤버면 204, 없으면 403  

Gateway 라우팅 **제외** (클러스터 내부만). Swagger Hidden 권장.

**Video는 Agit Redis를 직접 보지 않는다.** Topic만 Fact A+B를 판정하고 Video는 Topic HEAD만 호출한다.

### Video 쪽 호출

- Adapter: `RestTopicVideoAccessAdapter`
- Env: `TOPIC_INTERNAL_BASE_URL`, `TOPIC_INTERNAL_API_KEY`
- 호출 시점: playback / download URL 발급 직전 (`requireViewAccess`)
- Owner는 topic을 호출하지 않음

### 계약 예시

```bash
# 허용
curl -i -X HEAD \
  -H "X-Internal-Api-Key: $INTERNAL_API_KEY" \
  "$TOPIC_BASE/internal/v1/videos/{videoUuid}/access/{userUuid}"
# → 204

# 거부
# → 403
```

---

## 2. [필수] Topic — `video.uploaded` / attach 시 소유권 검증

### 왜 필요한가

Kafka `video.uploaded`의 `userUuid`를 그대로 믿으면, 이벤트 위조·재처리 시 **남의 영상을 토픽에 붙일 수 있음**.  
소유 진실은 video DB에만 있음.

없으면: attach 보안 구멍 (#4). 업로드/재생 자체는 동작.

### 무엇을 (video가 이미 제공 — topic이 호출)

```http
GET /internal/videos/{videoUuid}
X-Internal-Api-Key: <VIDEO_INTERNAL_API_KEY 또는 공통 INTERNAL_API_KEY>
```

| 응답 | Body |
| --- | --- |
| `200` | `{ "videoUuid": "...", "userUuid": "..." }` |
| `404` | 영상 없음 |

### 어떻게 (topic 측)

1. `video.uploaded` consume 또는 `POST attach` 시 위 API로 owner 조회  
2. `ownerUuid == event/request userUuid` 일 때만 `topic_video` insert  
3. video 내부 API 장애(5xx/timeout) 시 **스킵하지 말고** 재시도 가능하도록 예외를 다시 throw (Kafka retry)

Env (topic): `VIDEO_INTERNAL_BASE_URL`, `VIDEO_INTERNAL_API_KEY`

이벤트 계약: `docs/events/video.uploaded.v1.md`

---

## 3. [조건부] Agit — Membership Internal API (+ API Key)

### 왜 필요한가

Topic의 §1 재생 판정이 “해당 agit 활성 멤버인가?”를 알아야 합니다.  
Membership 진실은 agit에 있습니다.  
**Video는 agit를 직접 호출하지 않습니다.** Topic → Agit 연동입니다.

§1을 topic이 구현하려면 agit API가 보호·유지되어야 합니다.

### 무엇을 (기존에 가깝게)

```http
GET /internal/v1/agits/{agitUuid}/members
X-Internal-Api-Key: <AGIT_INTERNAL_API_KEY>
```

| 응답 | Body 예시 |
| --- | --- |
| `200` | `[{ "userUuid": "...", "role": "HOST\|MEMBER" }, ...]` |
| `401` | key 없음/불일치 |

### 어떻게

- Gateway 외부 노출 금지 (`/internal/**`)
- `X-Internal-Api-Key` constant-time 비교 권장
- Topic은 Redis 캐시 + miss 시 위 API warmup 패턴 사용 중이면 그 계약 유지

Env (topic): `AGIT_INTERNAL_BASE_URL`, `AGIT_INTERNAL_API_KEY`  
Env (agit): `INTERNAL_API_KEY` (또는 `AGIT_INTERNAL_API_KEY`) — **호출 측과 동일 값**

### Agit만 롤백해도 video 직접 장애는 없음

다만 topic이 §1을 구현한 뒤 agit key를 끄거나 API를 막으면 **멤버 재생이 다시 실패**합니다.  
§1 요청 시 agit 팀에도 “internal members + API key 유지”를 같이 전달하세요.

---

## 4. [선택] Gateway HMAC 검증 (topic / agit)

### 왜

Gateway가 `X-Gateway-Timestamp` + `X-Gateway-Signature`를 붙입니다.  
MSA가 검증하면 “gateway를 우회한 직접 `/api/**` 호출”을 막을 수 있습니다.

### Video / Gateway 관할

- **서명:** `plip-gateway` (`GATEWAY_HMAC_SECRET`)
- **검증:** `plip-video` (동일 secret)

### Topic / Agit

- secret이 비어 있으면 검증 비활성 (local OK)
- **검증 필터를 안 넣어도 video·gateway는 정상 동작** (헤더 무시)
- NetworkPolicy(manifest)로 Pod 직접 접근을 막는 것과 보완 관계

우선순위: §1·§2·§3 > HMAC 검증.

---

## 5. 요청 우선순위 요약

| 순위 | 담당 | 요청 | 없으면 |
| --- | --- | --- | --- |
| P0 | **topic** | §1 HEAD access API | 멤버 재생 불가 (업로더만 OK) |
| P0 | **topic** | §2 ownership 조회 후 attach | attach 보안 구멍 |
| P0 | **agit** | §3 members + API key (topic §1용) | topic §1 구현 불가/불완전 |
| P2 | topic/agit | §4 Gateway HMAC verify | 직접 MSA 호출 가능 (NetworkPolicy로 완화 가능) |

---

## 6. 환경 변수 맞춤표

서비스 간 **같은 키 문자열**을 맞춰야 합니다.

| 변수 | video | topic | agit |
| --- | --- | --- | --- |
| `INTERNAL_API_KEY` | 수신(own `/internal`) | 수신 | 수신 |
| `TOPIC_INTERNAL_API_KEY` | → topic 호출 | = 수신 키 | — |
| `VIDEO_INTERNAL_API_KEY` | = 수신 키 | → video 호출 | — |
| `AGIT_INTERNAL_API_KEY` | — | → agit 호출 | = 수신 키 |
| `GATEWAY_HMAC_SECRET` | 검증 | (선택) 검증 | (선택) 검증 |

---

## 7. Topic / Agit 팀에 붙여 넣을 짧은 요청문

### Topic 팀

> video 재생(비소유자)과 attach 보안을 위해 아래 두 가지가 필요합니다.
>
> 1. **Internal:** `HEAD /internal/v1/videos/{videoUuid}/access/{userUuid}`  
>    - `X-Internal-Api-Key` 필수  
>    - 해당 video가 붙은 topic의 agit에 `userUuid`가 활성 멤버면 204, 아니면 403  
>    - 상세: `plip-video/docs/api/cross-service-requests.md` §1  
>
> 2. **Consumer/API:** `video.uploaded` 및 attach 시  
>    `GET {VIDEO_INTERNAL}/internal/videos/{videoUuid}` 로 owner 확인 후 일치할 때만 attach  
>    - 상세: 동 문서 §2, 이벤트 `docs/events/video.uploaded.v1.md`  
>
> video는 업로더 본인 재생·ownership API·이벤트 발행까지 담당합니다.  
> membership 판정 API(§1)와 attach 시 ownership 호출(§2)만 topic 쪽 구현이 필요합니다.

### Agit 팀

> topic이 영상 재생 권한을 판정하려면 membership 조회가 필요합니다.
>
> - `GET /internal/v1/agits/{agitUuid}/members` 유지  
> - `X-Internal-Api-Key`로 `/internal/**` 보호  
> - topic의 `AGIT_INTERNAL_API_KEY`와 동일 값  
> - 상세: `plip-video/docs/api/cross-service-requests.md` §3  
>
> video는 agit를 직접 호출하지 않습니다.





아지트의 맴버 정보 api를 만들고 조회하기
-> 어차피 토픽에 대한 video는 해당 아지트의 맴버 모두가 볼 수 있음