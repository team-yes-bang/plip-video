# DDL for plip_video microservice (ERD 기준).
# 등록 위치(diary/agit)는 Agit/Diary 서비스가 video_uuid 로 참조 — video 테이블에 저장하지 않음.

CREATE TABLE video (
    id                    BIGINT       NOT NULL AUTO_INCREMENT COMMENT 'video_id (PK)',
    video_uuid            BINARY(16)   NOT NULL COMMENT 'UUIDv7',
    user_uuid             BINARY(16)   NOT NULL COMMENT 'Uploader UUID (UUIDv7)',
    caption               VARCHAR(100) NULL     COMMENT 'Short text input (optional)',
    file_path             VARCHAR(255) NOT NULL COMMENT 'Relative S3 path — raw video',
    processed_path        VARCHAR(255) NULL     COMMENT 'Relative S3 path — download-ready video',
    file_size_byte        BIGINT       NOT NULL COMMENT 'File size in bytes',
    thumbnail_image_path  VARCHAR(255) NULL     COMMENT 'Relative S3 path — NULL until thumbnail Lambda callback',
    created_at            DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT 'Upload time',
    updated_at            DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    deleted_at            DATETIME(6)  NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_video_uuid (video_uuid),
    INDEX idx_video_user_uuid (user_uuid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='5-second video metadata. All S3 paths are relative.';

CREATE TABLE image (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    title       VARCHAR(100) NULL     COMMENT 'Image title',
    status      VARCHAR(20)  NOT NULL COMMENT 'ACTIVE | DELETED',
    img_path    VARCHAR(200) NULL     COMMENT 'Relative S3 path',
    type        VARCHAR(20)  NOT NULL COMMENT 'PROFILE_IMG | THUMBNAIL | PRODUCT_IMG',
    file_type   VARCHAR(50)  NULL     COMMENT 'MIME or extension hint',
    created_at  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    deleted_at  DATETIME(6)  NULL,
    PRIMARY KEY (id),
    INDEX idx_image_type_status (type, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Master image table (ERD). Profile/Product path는 각 서비스 DB';

CREATE TABLE video_processing_outbox (
    id                BIGINT       NOT NULL AUTO_INCREMENT,
    video_uuid        BINARY(16)   NOT NULL COMMENT 'Target video UUID',
    event_type        VARCHAR(32)  NOT NULL COMMENT 'THUMBNAIL_INVOKE | SQS_ENQUEUE',
    payload_json      JSON         NOT NULL,
    status            VARCHAR(16)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING | SENT | FAILED',
    attempt_count     INT          NOT NULL DEFAULT 0,
    next_attempt_at   DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    created_at        DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    sent_at           DATETIME(6)  NULL,
    last_error        VARCHAR(512) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_outbox_video_event (video_uuid, event_type),
    INDEX idx_outbox_pending (status, next_attempt_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Durable outbox for post-complete Lambda/SQS dispatch';

CREATE TABLE video_destination_outbox (
    id                BIGINT       NOT NULL AUTO_INCREMENT,
    video_uuid        BINARY(16)   NOT NULL,
    event_type        VARCHAR(32)  NOT NULL COMMENT 'TOPIC_VIDEO_UPLOADED | DIARY_VIDEO_UPLOADED',
    destination_uuid  BINARY(16)   NOT NULL COMMENT 'topicUuid or themeUuid',
    payload_json      JSON         NOT NULL,
    status            VARCHAR(16)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING | SENT | FAILED',
    attempt_count     INT          NOT NULL DEFAULT 0,
    next_attempt_at   DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    created_at        DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    sent_at           DATETIME(6)  NULL,
    last_error        VARCHAR(512) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_destination_outbox (video_uuid, event_type, destination_uuid),
    INDEX idx_destination_outbox_pending (status, next_attempt_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Durable outbox for destination Kafka events';
