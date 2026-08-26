# Upload size enforcement (video-service)

## Goal

Prevent clients from uploading objects larger than `plip.video.max-file-size-bytes` **at S3 PUT time**,
not only at `complete` (HeadObject).

## Mechanism

1. Client knows `blob.size` before upload.
2. `POST /api/v1/videos/upload-url?contentLengthBytes={size}` — server rejects if `size > max` or `size <= 0`.
3. Presigned PUT signs `Content-Length: {size}` (AWS SigV4).
4. Client `PUT` must send the same length; otherwise S3 returns `403 SignatureDoesNotMatch` / related error.
5. `complete` still runs HeadObject size check (defense in depth).

## Why not Presigned POST `content-length-range`?

POST policy supports a range, but changes the upload protocol (form fields).  
Signed PUT `Content-Length` keeps the existing PUT flow and is exact-size bound.

## Client contract

| Step | API |
| --- | --- |
| Issue | `contentLengthBytes` **required** query param |
| Upload | `PUT` body length must equal issued size |
| Complete | unchanged |

Front: `issueUploadUrlAction(contentType, blob.size)` → `putPresignedUpload(url, blob, contentType)`.
