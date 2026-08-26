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

## Phase 1b thumbnail Lambda

- 코드: `terraform/lambda/thumbnail/handler.py` (FFmpeg 첫 프레임 + processed S3 + callback)
- 배포: `terraform/lambda/thumbnail/deploy.sh`
- **ECR policy (admin 1회)**: `terraform/lambda/thumbnail/set-ecr-policy.sh` — `ecr:SetRepositoryPolicy` 권한 필요
- K8s: `infra/ingress/fo-ingress.yaml` (`/internal/videos`), `infra/network-policy/msa-ingress.yaml` (Traefik)
- E2E: 아래 Thumbnail 테스트

### Thumbnail Lambda 배포 순서

```bash
# 1. ECR import (deploy.sh가 repo를 먼저 만든 경우)
cd terraform
terraform import 'aws_ecr_repository.video_thumbnail[0]' plip-video-thumbnail

# 2. Admin: ECR → Lambda pull policy (teamYES 권한 없으면 root/admin 계정)
./lambda/thumbnail/set-ecr-policy.sh

# 3. Image Lambda 생성
terraform apply -target='aws_lambda_function.thumbnail[0]'

# 4. 이미지 빌드/푸시/갱신
cd lambda/thumbnail && ./deploy.sh
```

## 아직 코드로 미완 (낮은 우선)

- (없음 — thumbnail Phase 1b 구현 완료, burn-in은 Phase 2)

## Thumbnail E2E 테스트

1. `INTERNAL_API_KEY` SSM ↔ video Secret 동기화 (phase2-e2e-test.md 0단계)
2. `kubectl apply -f plip-k8s-manifests/infra/ingress/fo-ingress.yaml`
3. `kubectl apply -f plip-k8s-manifests/infra/network-policy/msa-ingress.yaml`
4. 영상 upload → complete
5. 확인:
   - Lambda CloudWatch: `thumbnail start`, `Thumbnail callback succeeded`
   - S3 processed: `aws s3 ls s3://<processed-bucket>/thumbnail/`
   - DB: `SELECT thumbnail_image_path FROM video ORDER BY created_at DESC LIMIT 1;`
   - API: `GET /api/videos/{uuid}` → `thumbnailUrl` (CDN)

## Phase 2 burn-in Lambda

- 코드: `terraform/lambda/burn_in/handler.py` (FFmpeg drawtext + callback)
- 배포: `terraform/lambda/burn_in/deploy.sh`
- E2E: `docs/api/phase2-e2e-test.md`
