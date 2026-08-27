package com.plip.video.application.port.in.dto;

import java.util.UUID;

public record VideoThumbnailUploadUrlCommand(
		UUID videoUuid,
		UUID userUuid,
		String contentType,
		Long contentLengthBytes
) {
}
