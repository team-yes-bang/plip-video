# Phase 2 E2E 테스트 가이드 (burn-in → download-url)

프론트 없이 **백엔드 + Lambda + AWS** 만으로 Phase 2 완료를 검증하는 절차입니다.

## 사전 조건

| 항목 | 값 / 확인 |
| --- | --- |
| Phase 1a | raw bucket 업로드 가능 (`enable_raw_media_bucket = true`) |
| Phase 2 infra | `enable_media_pipeline = true` (SQS, processed bucket, CloudFront) |
| plip-video | `AWS_ENABLED=true`, SQS URL, processed bucket, CDN base URL 설정 |
| Burn-in Lambda | Container image 배포됨 (`terraform/lambda/burn_in/deploy.sh`) |
| INTERNAL_API_KEY | Lambda env ↔ plip-video **동일 값** (아래 0단계) |

---

## 0. INTERNAL_API_KEY 동기화 (필수)

Lambda callback이 plip-video에 도달하려면 양쪽 키가 일치해야 합니다.

```bash
export AWS_REGION=ap-northeast-2

# SSM 값 조회
export INTERNAL_API_KEY=$(aws ssm get-parameter \
  --name /yes-plip/media/lambda-internal-api-key \
  --with-decryption \
  --query Parameter.Value \
  --output text)

echo "Key length: ${#INTERNAL_API_KEY}"
```

**로컬 plip-video bootRun:**

```bash
# back/plip-video/.env 또는 IDE env
INTERNAL_API_KEY=<위 SSM 값>
AWS_ENABLED=true
AWS_S3_RAW_BUCKET=yes-plip-dev-media-334687118442-raw
AWS_S3_PROCESSED_BUCKET=yes-plip-dev-media-334687118442-processed
AWS_SQS_VIDEO_PROCESSING_QUEUE_URL=https://sqs.ap-northeast-2.amazonaws.com/334687118442/yes-plip-dev-video-processing
AWS_CDN_BASE_URL=https://ds2468g03st3p.cloudfront.net
```

> 로컬 bootRun 시 Lambda callback URL은 Terraform 기본값 `https://fo-api.plip.life` 입니다.  
> **로컬 DB만 쓰는 경우** callback은 K8s/prod plip-video로 가므로, 로컬 DB 검증은 **수동 callback(3-C)** 또는 **K8s plip-video** 를 사용하세요.

**K8s plip-video:**

```bash
kubectl -n plip patch secret video-aws-credentials \
  --type merge \
  -p "{\"stringData\":{\"INTERNAL_API_KEY\":\"${INTERNAL_API_KEY}\"}}"
kubectl -n plip rollout restart deployment/plip-video
```

---

## 1. Burn-in Lambda 배포

```bash
cd terraform/lambda/burn_in

# 최초 (zip → Image 전환 시)
PUSH_ONLY=1 ./deploy.sh
cd ../.. && terraform apply
cd lambda/burn_in && ./deploy.sh

# 이후 코드 변경 시
./deploy.sh
```

**확인:**

```bash
aws lambda get-function \
  --function-name yes-plip-dev-video-burnin \
  --region ap-northeast-2 \
  --query 'Configuration.[PackageType,LastUpdateStatus,State]'
# PackageType: Image, State: Active
```

---

## 2. 백엔드 단위 테스트

```bash
cd back/plip-video
./gradlew test --tests '*DownloadUrl*' --tests InternalVideoControllerTest
```

---

## 3. E2E 시나리오

### 3-A. 전체 파이프라인 (upload → SQS → Lambda → download-url)

**1) upload-url 발급**

```bash
export USER_UUID="<로그인 사용자 UUID>"
export CONTENT_LENGTH=500000   # 실제 mp4 바이트 수과 일치해야 함

curl -s -X POST \
  "http://localhost:8085/api/v1/videos/upload-url?contentType=video/mp4&contentLengthBytes=${CONTENT_LENGTH}" \
  -H "X-User-UUID: ${USER_UUID}" | tee /tmp/upload-url.json

export VIDEO_UUID=$(jq -r '.videoUuid' /tmp/upload-url.json)
export UPLOAD_URL=$(jq -r '.uploadUrl' /tmp/upload-url.json)
echo "videoUuid=${VIDEO_UUID}"
```

**2) S3 Presigned PUT**

```bash
curl -X PUT "${UPLOAD_URL}" \
  -H "Content-Type: video/mp4" \
  --data-binary @/path/to/sample-5s.mp4
```

**3) complete (outbox → SQS enqueue)**

```bash
curl -s -X POST "http://localhost:8085/api/v1/videos/${VIDEO_UUID}/complete" \
  -H "Content-Type: application/json" \
  -H "X-User-UUID: ${USER_UUID}" \
  -d '{"caption":"Phase2 E2E"}' | jq .
```

**4) plip-video 로그 확인 (5~10초 내)**

```text
Enqueued video processing job for {videoUuid}
```

**5) SQS / Lambda 확인**

```bash
# 큐에 메시지가 잠깐 있다가 Lambda가 소비
aws sqs get-queue-attributes \
  --queue-url https://sqs.ap-northeast-2.amazonaws.com/334687118442/yes-plip-dev-video-processing \
  --attribute-names ApproximateNumberOfMessages ApproximateNumberOfMessagesNotVisible \
  --region ap-northeast-2

# CloudWatch Logs (최근 5분)
aws logs tail /aws/lambda/yes-plip-dev-video-burnin --since 5m --region ap-northeast-2
```

**기대 로그:**

```text
burn-in start videoUuid=...
Processed callback succeeded videoUuid=...
status=COMPLETED
```

**6) processed S3 객체 확인**

```bash
aws s3 ls "s3://yes-plip-dev-media-334687118442-processed/videos/processed/${VIDEO_UUID}.mp4" \
  --region ap-northeast-2
```

**7) download-url — 가공 전 (선택)**

```bash
curl -i "http://localhost:8085/api/v1/videos/${VIDEO_UUID}/download-url" \
  -H "X-User-UUID: ${USER_UUID}"
# HTTP 202, Retry-After: 3, status: PROCESSING
```

**8) download-url — 가공 후**

```bash
# Lambda 완료 후 (보통 10~60초)
curl -s "http://localhost:8085/api/v1/videos/${VIDEO_UUID}/download-url" \
  -H "X-User-UUID: ${USER_UUID}" | jq .
```

**기대:**

```json
{
  "videoUuid": "...",
  "downloadUrl": "https://ds2468g03st3p.cloudfront.net/videos/processed/{uuid}.mp4"
}
```

**9) GET detail**

```bash
curl -s "http://localhost:8085/api/v1/videos/${VIDEO_UUID}" \
  -H "X-User-UUID: ${USER_UUID}" | jq '{downloadReady, thumbnailUrl}'
# downloadReady: true
```

**10) CloudFront 파일 재생 확인**

```bash
curl -I "$(curl -s "http://localhost:8085/api/v1/videos/${VIDEO_UUID}/download-url" \
  -H "X-User-UUID: ${USER_UUID}" | jq -r '.downloadUrl')"
# HTTP 200, Content-Type: video/mp4
```

브라우저에서 `downloadUrl`을 열어 **시간(HH:mm) + 캡션** burn-in 여부를 확인합니다.

---

### 3-B. Lambda 직접 invoke (SQS 없이)

raw S3에 파일이 이미 있을 때:

```bash
cd terraform/lambda/burn_in
# sample-sqs-record.json 의 REPLACE-WITH-UUID 교체 후
aws lambda invoke \
  --function-name yes-plip-dev-video-burnin \
  --region ap-northeast-2 \
  --payload file://sample-sqs-record.json \
  --cli-binary-format raw-in-base64-out \
  /tmp/burnin-out.json && cat /tmp/burnin-out.json
```

---

### 3-C. callback 경로만 검증 (Lambda 없이)

plip-video API + download-url 200 전환만 빠르게 확인:

```bash
curl -X PATCH "http://localhost:8085/internal/videos/${VIDEO_UUID}/processed" \
  -H "X-Internal-Api-Key: ${INTERNAL_API_KEY}" \
  -H "Content-Type: application/json" \
  -d "{\"processedS3Key\":\"videos/processed/${VIDEO_UUID}.mp4\",\"durationSeconds\":4}"

curl -s "http://localhost:8085/api/v1/videos/${VIDEO_UUID}/download-url" \
  -H "X-User-UUID: ${USER_UUID}" | jq .
# status 없이 downloadUrl만 (200)
```

---

## 4. 실패 시 진단

| 증상 | 확인 |
| --- | --- |
| SQS 메시지 쌓임, Lambda 미실행 | event source mapping enabled, Lambda State=Active |
| Lambda `FileNotFoundError: Font` | Image 재빌드/재배포 (`./deploy.sh`) |
| Lambda `ffprobe/ffmpeg not found` | Container image가 zip이 아닌지 확인 |
| Lambda callback HTTP 401 | INTERNAL_API_KEY 불일치 |
| Lambda callback HTTP 404 | videoUuid DB에 없음 (complete 안 함) |
| download-url 영구 202 | callback 미수신 — CloudWatch + plip-video 로그 |
| CloudFront 403 | processed bucket 객체 없음 또는 OAC mismatch |
| DLQ 메시지 | CloudWatch error 로그 → duration exceeded / ffmpeg fail |

**DLQ 확인:**

```bash
aws sqs get-queue-attributes \
  --queue-url https://sqs.ap-northeast-2.amazonaws.com/334687118442/yes-plip-dev-video-processing-dlq \
  --attribute-names ApproximateNumberOfMessages \
  --region ap-northeast-2
```

---

## 5. Phase 2 완료 체크리스트

- [ ] `./deploy.sh` 후 Lambda `PackageType=Image`
- [ ] complete → SQS enqueue 로그
- [ ] CloudWatch `status=COMPLETED`
- [ ] processed bucket에 `{uuid}.mp4` 존재
- [ ] `PATCH /internal/.../processed` 204 (또는 Lambda callback success 로그)
- [ ] `GET .../download-url` → 200 + CloudFront URL
- [ ] CloudFront URL에서 burn-in 영상 재생
- [ ] DLQ 메시지 0

---

## 6. Python unit test (Lambda 코드)

```bash
cd terraform/lambda/burn_in
python3 -m unittest test_handler.py -v
```
