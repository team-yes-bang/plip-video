package com.plip.video.adapter.in.web.dto;

import java.util.UUID;

public record InternalVideoOwnershipResponse(
		UUID videoUuid,
		UUID userUuid
) {
}
