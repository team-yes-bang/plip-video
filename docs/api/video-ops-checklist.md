# Video / Gateway / Manifest — 운영·로컬 체크리스트 (관할)

코드로 끝난 항목과, **값 주입·플래그만** 남은 운영 작업을 구분한다.

## 이미 코드에 있는 것

- Owner Fast-Pass + Topic HEAD access (`RestTopicVideoAccessAdapter`)
- `GET /internal/videos/{videoUuid}` ownership + `X-Internal-Api-Key`
- Gateway HMAC 서명(gateway) / 검증(video) — secret 비어 있으면 비활성
- NetworkPolicy: `infra/network-policy/msa-ingress.yaml`
- Upload size: `contentLengthBytes` 서명 + complete HeadObject
- Processed callback: `durationSeconds` 필수 + max 검증
- Front: query `userUuid` 제거, 세션 헤더 `X-User-UUID` 사용

## 운영에서 해야 할 것 (코드 변경 없음)

### 1. Gateway HMAC secret (prod/K8s)

```bash
# SSM에 공유 secret 등록 (예시)
aws ssm put-parameter \
  --name /plip/gateway-hmac-secret \
  --type SecureString \
  --value '<strong-random-secret>' \
  --region ap-northeast-2

# CronJob 즉시 실행
kubectl -n plip create job ssm-secret-sync-now --from=cronjob/ssm-secret-sync

# 주입 확인
kubectl -n plip get secret plip-gateway-hmac -o jsonpath='{.data.GATEWAY_HMAC_SECRET}' | base64 -d; echo
kubectl -n plip get secret video-aws-credentials -o jsonpath='{.data.GATEWAY_HMAC_SECRET}' | base64 -d; echo
```

gateway Deployment·video Secret은 이미 `GATEWAY_HMAC_SECRET`을 읽도록 배선됨.

로컬: `.env`에서 `GATEWAY_HMAC_SECRET=` 비우면 검증 OFF (direct `:8085` OK).

### 2. Front gateway 라우팅

| 환경 | 권장 |
| --- | --- |
| 로컬 MSA direct | `VIDEO_USE_GATEWAY=false`, `VIDEO_API_BASE_URL=http://localhost:8085` |
| develop / prod | `VIDEO_USE_GATEWAY=true`, `API_URL=<gateway>` |

### 3. Topic / Agit (관할 밖 — 요청만)

- Topic: `HEAD /internal/v1/videos/{uuid}/access/{userUuid}` — **RDB Fact A + Redis Fact B**
- Topic: attach 시 `GET /internal/videos/{uuid}` ownership
- Agit: Redis Set + internal members + API key

상세: `docs/api/cross-service-requests.md`

## 아직 코드로 미완 (낮은 우선)

- Thumbnail Lambda: S3 + FFmpeg 첫 프레임 (Phase 1b skeleton)

## Phase 2 burn-in Lambda

- 코드: `terraform/lambda/burn_in/handler.py` (FFmpeg drawtext + callback)
- 배포: `terraform/lambda/burn_in/deploy.sh`
- E2E: `docs/api/phase2-e2e-test.md`
