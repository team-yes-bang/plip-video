package com.plip.video.application.port.in.dto;

import java.util.UUID;

public record VideoOwnershipResult(
		UUID videoUuid,
		UUID userUuid
) {
}
