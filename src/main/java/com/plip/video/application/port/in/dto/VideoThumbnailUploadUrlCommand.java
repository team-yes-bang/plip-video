package com.plip.video.application.port.in.dto;

import java.util.UUID;

public record VideoThumbnailUploadUrlCommand(
		UUID userUuid,
		UUID videoUuid,
		String contentType,
		Long contentLengthBytes
) {
}
