package com.plip.video.application.port.in.dto;

import java.util.UUID;

public record VideoUploadUrlCommand(
		UUID userUuid,
		String contentType
) {
}
