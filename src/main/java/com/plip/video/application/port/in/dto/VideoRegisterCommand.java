package com.plip.video.application.port.in.dto;

import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public record VideoRegisterCommand(
		UUID userUuid,
		MultipartFile videoFile,
		String caption
) {
}
